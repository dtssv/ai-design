package com.aicopilot.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aicopilot.api.common.exception.BizException;
import com.aicopilot.api.common.result.ResultCode;
import com.aicopilot.api.dto.ai.GenerateRequest;
import com.aicopilot.api.dto.ai.SseEvent;
import com.aicopilot.api.entity.ApiKey;
import com.aicopilot.api.entity.CodeSnapshot;
import com.aicopilot.api.entity.Conversation;
import com.aicopilot.api.entity.KnowledgeBase;
import com.aicopilot.api.entity.KnowledgeEntry;
import com.aicopilot.api.entity.Message;
import com.aicopilot.api.mapper.KnowledgeBaseMapper;
import com.aicopilot.api.mapper.KnowledgeEntryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

/**
 * AI 代码生成服务（核心）
 *
 * 核心设计：
 * 1. 增量生成 - 每次对话基于最新代码快照修改，而非从零生成
 * 2. 结构化输出 - 通过 <<<AI_CODE_OUTPUT>>> JSON标记提取文件，替代脆弱的正则
 * 3. 对话摘要 - 超过阈值时压缩历史对话，避免超Token
 * 4. 多模态支持 - 图片URL作为vision输入（设计稿/截图驱动生成）
 * 5. 项目结构约束 - System Prompt 严格规定文件组织和命名规范
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationService {

    private final ApiKeyPoolService apiKeyPoolService;
    private final ConversationService conversationService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeEntryMapper knowledgeEntryMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final UsageRecordService usageRecordService;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key-pool.retry-count:3}")
    private int retryCount;

    @Value("${ai.api-key-pool.timeout-seconds:120}")
    private int timeoutSeconds;

    /** 触发对话摘要压缩的消息数阈值 */
    private static final int SUMMARY_THRESHOLD = 10;
    /** 保留最近N条完整消息（不被摘要压缩） */
    private static final int RECENT_MESSAGES_KEEP = 6;
    /** 结构化输出标记 - 开始 */
    private static final String CODE_OUTPUT_START = "<<<AI_CODE_OUTPUT>>>";
    /** 结构化输出标记 - 结束 */
    private static final String CODE_OUTPUT_END = "<<<END_AI_CODE_OUTPUT>>>";

    // ==================== 公开方法 ====================

    /**
     * 流式生成代码 - 核心入口
     */
    public SseEmitter generateStream(Long conversationId, GenerateRequest request, Long userId) {
        SseEmitter emitter = new SseEmitter((long) timeoutSeconds * 1000);

        CompletableFuture.runAsync(() -> {
            try {
                doGenerate(conversationId, request, userId, emitter);
            } catch (Exception e) {
                log.error("AI生成异常, conversationId={}", conversationId, e);
                sendSseEvent(emitter, SseEvent.builder().type("error").error(e.getMessage()).build());
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.warn("SSE连接异常断开: {}", e.getMessage()));
        return emitter;
    }

    /**
     * 原型转开发模式（SSE流式）
     */
    public SseEmitter upgradeToDevMode(Long conversationId, Long userId) {
        SseEmitter emitter = new SseEmitter((long) timeoutSeconds * 1000);

        CompletableFuture.runAsync(() -> {
            try {
                doUpgrade(conversationId, userId, emitter);
            } catch (Exception e) {
                log.error("原型转开发模式异常, conversationId={}", conversationId, e);
                sendSseEvent(emitter, SseEvent.builder().type("error").error(e.getMessage()).build());
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.warn("SSE连接异常断开: {}", e.getMessage()));
        return emitter;
    }

    // ==================== 核心生成流程 ====================

    private void doGenerate(Long conversationId, GenerateRequest request, Long userId, SseEmitter emitter) {
        // 1. 查询会话
        Conversation conversation = conversationService.getById(conversationId);
        if (conversation == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }

        // 2. 保存用户消息（含附件信息）
        String attachmentsJson = buildAttachmentsJson(request);
        conversationService.saveUserMessage(conversationId, request.getContent(), attachmentsJson);

        // 3. 选择API Key
        ApiKey selectedKey = apiKeyPoolService.selectKey(
                conversation.getApiKeySource(), conversation.getApiKeyId(), request.getModelName());

        // 4. 构建Prompt（含最新快照、摘要、多模态）
        List<Map<String, Object>> promptMessages = buildPromptMessages(conversation, request);

        // 5. 流式调用大模型（带重试）
        StringBuilder fullContent = new StringBuilder();
        AtomicInteger totalTokens = new AtomicInteger(0);
        callModelApiWithRetry(selectedKey, promptMessages, emitter, fullContent, totalTokens);

        // 6. 解析结构化输出 → 合并到最新快照
        String responseText = fullContent.toString();
        CodeSnapshot latestSnapshot = conversationService.getLatestSnapshot(conversationId);
        String mergedFiles = mergeCodeFiles(responseText, latestSnapshot);
        List<String> changedFilePaths = extractChangedFilePaths(responseText);

        // 7. 保存AI消息 + 代码快照
        Message aiMsg = conversationService.saveAssistantMessage(
                conversationId, responseText,
                selectedKey.getModelName(), totalTokens.get(),
                conversation.getWorkspaceId(), mergedFiles, conversation.getGenerationMode());

        // 8. 记录用量
        apiKeyPoolService.recordKeyUsage(selectedKey.getId(), totalTokens.get());
        usageRecordService.recordUsage(userId, conversation, aiMsg, selectedKey, totalTokens.get());

        // 9. 异步更新对话摘要（消息数超阈值时压缩）
        updateSummaryIfNeeded(conversation);

        // 10. 生成摘要文本
        String turnSummary = buildTurnSummary(request.getContent(), changedFilePaths);

        // 11. 发送完成事件
        CodeSnapshot newSnapshot = conversationService.getLatestSnapshot(conversationId);
        sendSseEvent(emitter, SseEvent.builder()
                .type("done")
                .messageId(aiMsg.getId())
                .snapshotId(newSnapshot != null ? newSnapshot.getId() : null)
                .snapshotVersion(newSnapshot != null ? newSnapshot.getVersion() : null)
                .model(selectedKey.getModelName())
                .tokenUsage(totalTokens.get())
                .changedFiles(changedFilePaths)
                .summary(turnSummary)
                .build());

        emitter.complete();
    }

    private void doUpgrade(Long conversationId, Long userId, SseEmitter emitter) {
        Conversation conversation = conversationService.getById(conversationId);
        if (conversation == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }

        CodeSnapshot latestSnapshot = conversationService.getLatestSnapshot(conversationId);
        String prototypeFiles = latestSnapshot != null ? latestSnapshot.getFiles() : "{}";

        conversationService.switchMode(conversationId, "development");
        conversation.setGenerationMode("development");

        GenerateRequest upgradeRequest = new GenerateRequest();
        upgradeRequest.setContent(
                "请将以下原型代码转换为生产级的React + TypeScript项目代码。\n" +
                        "要求：保持UI设计和交互不变，拆分为组件化结构，添加TypeScript类型。\n" +
                        "当前原型文件：\n" + prototypeFiles);

        ApiKey selectedKey = apiKeyPoolService.selectKey(
                conversation.getApiKeySource(), conversation.getApiKeyId(), null);
        List<Map<String, Object>> promptMessages = buildPromptMessages(conversation, upgradeRequest);

        StringBuilder fullContent = new StringBuilder();
        AtomicInteger totalTokens = new AtomicInteger(0);
        callModelApiWithRetry(selectedKey, promptMessages, emitter, fullContent, totalTokens);

        String responseText = fullContent.toString();
        // upgrade模式不做合并，完全重新生成
        String codeFiles = extractCodeFilesFromResponse(responseText);
        Message aiMsg = conversationService.saveAssistantMessage(
                conversationId, responseText, selectedKey.getModelName(), totalTokens.get(),
                conversation.getWorkspaceId(), codeFiles, "development");

        apiKeyPoolService.recordKeyUsage(selectedKey.getId(), totalTokens.get());
        usageRecordService.recordUsage(userId, conversation, aiMsg, selectedKey, totalTokens.get());

        CodeSnapshot newSnapshot = conversationService.getLatestSnapshot(conversationId);
        sendSseEvent(emitter, SseEvent.builder()
                .type("done")
                .messageId(aiMsg.getId())
                .snapshotId(newSnapshot != null ? newSnapshot.getId() : null)
                .snapshotVersion(newSnapshot != null ? newSnapshot.getVersion() : null)
                .model(selectedKey.getModelName())
                .tokenUsage(totalTokens.get())
                .summary("原型已转换为React+TypeScript开发模式代码")
                .build());
        emitter.complete();
    }

    // ==================== Prompt 构建 ====================

    /**
     * 构建完整的Prompt消息列表（支持多模态）
     *
     * 结构：System Prompt → [摘要] → 最近消息 → 最新快照 → 用户输入[+图片]
     */
    private List<Map<String, Object>> buildPromptMessages(Conversation conversation, GenerateRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. System Prompt（含项目结构约束 + 输出格式 + 知识库）
        String systemPrompt = buildSystemPrompt(conversation.getGenerationMode(), request.getKnowledgeBaseIds());
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // 2. 对话摘要（如果有）
        if (conversation.getContextSummary() != null && !conversation.getContextSummary().isBlank()) {
            messages.add(Map.of("role", "system", "content",
                    "[对话历史摘要]\n" + conversation.getContextSummary()));
        }

        // 3. 最近的完整消息（保留最近几轮，避免全量历史超Token）
        List<Message> recentMessages = conversationService.getRecentMessages(
                conversation.getId(), RECENT_MESSAGES_KEEP);
        for (Message msg : recentMessages) {
            if (msg.getContent() != null) {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }

        // 4. 注入最新代码快照（让AI基于现有代码增量修改）
        CodeSnapshot latestSnapshot = conversationService.getLatestSnapshot(conversation.getId());
        if (latestSnapshot != null && latestSnapshot.getFiles() != null) {
            String currentCodeContext = buildCurrentCodeContext(latestSnapshot);
            messages.add(Map.of("role", "system", "content", currentCodeContext));
        }

        // 5. 用户输入（支持多模态：文本 + 图片）
        messages.add(buildUserMessage(request));

        return messages;
    }

    /**
     * 构建系统Prompt - 详细约束项目结构、命名规范、输出格式
     */
    private String buildSystemPrompt(String generationMode, List<Long> knowledgeBaseIds) {
        StringBuilder sb = new StringBuilder();

        // ======= 角色定义 =======
        sb.append("# 角色\n");
        sb.append("你是一个资深的前端架构师和开发专家，专注于根据用户需求生成高质量的前端代码。\n\n");

        // ======= 核心工作模式 =======
        sb.append("# 核心工作模式\n");
        sb.append("- **增量修改**：你收到的上下文中包含当前项目的完整代码快照，你必须基于现有代码进行修改，而不是每次重新生成全部文件。\n");
        sb.append("- **最小变更原则**：只修改需要变更的文件，未提及的文件保持不变。\n");
        sb.append("- **先总结再输出**：回答时先用自然语言简要说明你做了什么修改、为什么这样改，然后再输出代码。\n\n");

        // ======= 生成模式 =======
        if ("prototype".equals(generationMode)) {
            sb.append("# 生成模式：原型模式\n");
            sb.append("生成快速原型页面，用于验证设计和交互。\n\n");
            sb.append("## 项目结构约束\n");
            sb.append("```\n");
            sb.append("src/\n");
            sb.append("  index.html          -- 主入口HTML\n");
            sb.append("  styles.css          -- 全局样式（可选）\n");
            sb.append("  app.js              -- 交互逻辑（可选）\n");
            sb.append("```\n\n");
            sb.append("## 技术要求\n");
            sb.append("- 优先使用单个HTML文件（内联CSS/JS），复杂页面可拆分\n");
            sb.append("- 使用现代UI设计：圆角、渐变、阴影、动画过渡\n");
            sb.append("- 必须支持响应式布局（移动端+桌面端）\n");
            sb.append("- 使用中文界面\n");
            sb.append("- 使用语义化HTML标签\n");
            sb.append("- 可引用CDN资源（如Tailwind CSS、Font Awesome等）\n\n");
        } else {
            sb.append("# 生成模式：开发模式\n");
            sb.append("生成可部署的生产级React + TypeScript代码。\n\n");
            sb.append("## 项目结构约束\n");
            sb.append("```\n");
            sb.append("src/\n");
            sb.append("  main.tsx                    -- 应用入口\n");
            sb.append("  App.tsx                     -- 根组件（路由配置）\n");
            sb.append("  index.css                   -- 全局样式/CSS变量\n");
            sb.append("  components/                 -- 通用组件\n");
            sb.append("    Button/\n");
            sb.append("      Button.tsx\n");
            sb.append("      Button.module.css\n");
            sb.append("      index.ts                -- 导出文件\n");
            sb.append("  pages/                      -- 页面组件\n");
            sb.append("    Home/\n");
            sb.append("      Home.tsx\n");
            sb.append("      Home.module.css\n");
            sb.append("  hooks/                      -- 自定义Hook\n");
            sb.append("  utils/                      -- 工具函数\n");
            sb.append("  types/                      -- TypeScript类型定义\n");
            sb.append("    index.ts\n");
            sb.append("  services/                   -- API请求封装\n");
            sb.append("  stores/                     -- 状态管理\n");
            sb.append("```\n\n");
            sb.append("## 技术要求\n");
            sb.append("- React 18 + TypeScript 5.x\n");
            sb.append("- CSS Modules 进行样式隔离（文件名: *.module.css）\n");
            sb.append("- 每个组件一个目录，含组件文件+样式+index.ts导出\n");
            sb.append("- 所有Props必须定义interface，使用有意义的命名\n");
            sb.append("- 使用函数式组件 + Hooks\n");
            sb.append("- 遵循单一职责原则，组件不超过200行\n");
            sb.append("- 使用中文界面\n\n");
        }

        // ======= 输出格式约束（结构化标记） =======
        sb.append("# 输出格式（必须严格遵守）\n\n");
        sb.append("## 1. 先输出修改说明\n");
        sb.append("用自然语言简要说明本次做了哪些修改，修改原因。\n\n");
        sb.append("## 2. 再输出代码文件\n");
        sb.append("所有代码文件必须包裹在特定标记内，使用JSON数组格式：\n\n");
        sb.append("```\n");
        sb.append("<<<AI_CODE_OUTPUT>>>\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"path\": \"src/components/Button/Button.tsx\",\n");
        sb.append("    \"action\": \"create\",\n");
        sb.append("    \"language\": \"tsx\",\n");
        sb.append("    \"content\": \"文件完整内容...\"\n");
        sb.append("  },\n");
        sb.append("  {\n");
        sb.append("    \"path\": \"src/pages/Home/Home.tsx\",\n");
        sb.append("    \"action\": \"modify\",\n");
        sb.append("    \"language\": \"tsx\",\n");
        sb.append("    \"content\": \"修改后的完整文件内容...\"\n");
        sb.append("  },\n");
        sb.append("  {\n");
        sb.append("    \"path\": \"src/utils/old.ts\",\n");
        sb.append("    \"action\": \"delete\"\n");
        sb.append("  }\n");
        sb.append("]\n");
        sb.append("<<<END_AI_CODE_OUTPUT>>>\n");
        sb.append("```\n\n");
        sb.append("## 字段说明\n");
        sb.append("- `path`: 相对于项目根目录的文件路径，必须以 src/ 开头\n");
        sb.append("- `action`: 操作类型，可选值：create(新建) / modify(修改) / delete(删除)\n");
        sb.append("- `language`: 文件语言（tsx/ts/css/html/json/js），delete操作时可省略\n");
        sb.append("- `content`: 文件的**完整内容**（不是diff），delete操作时可省略\n\n");
        sb.append("## 重要规则\n");
        sb.append("- 每个被修改的文件必须输出完整内容，不要用省略号或注释省略\n");
        sb.append("- 未修改的文件不要输出\n");
        sb.append("- 标记 <<<AI_CODE_OUTPUT>>> 和 <<<END_AI_CODE_OUTPUT>>> 必须独占一行\n");
        sb.append("- JSON必须合法，字符串中的换行用 \\n 转义，双引号用 \\\" 转义\n\n");

        // ======= 知识库注入 =======
        appendKnowledgeContext(sb, knowledgeBaseIds);

        return sb.toString();
    }

    /**
     * 构建当前代码上下文（注入最新快照让AI知道现有代码）
     */
    private String buildCurrentCodeContext(CodeSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("[当前项目代码快照 v").append(snapshot.getVersion()).append("]\n");
        sb.append("以下是项目的当前代码文件，请基于这些文件进行增量修改，只输出需要变更的文件：\n\n");

        try {
            List<Map<String, String>> files = objectMapper.readValue(
                    snapshot.getFiles(), new TypeReference<List<Map<String, String>>>() {
                    });
            for (Map<String, String> file : files) {
                String path = file.get("path");
                String lang = file.get("language");
                String content = file.get("content");
                sb.append("### ").append(path).append("\n");
                sb.append("```").append(lang != null ? lang : "").append("\n");
                sb.append(content).append("\n```\n\n");
            }
        } catch (Exception e) {
            // 如果解析失败，直接附加原始JSON
            sb.append(snapshot.getFiles());
        }

        return sb.toString();
    }

    /**
     * 构建用户消息（支持多模态：文本 + 图片）
     */
    private Map<String, Object> buildUserMessage(GenerateRequest request) {
        List<String> imageUrls = request.getImageUrls();
        boolean hasImages = imageUrls != null && !imageUrls.isEmpty();

        if (!hasImages) {
            // 纯文本消息
            return Map.of("role", "user", "content", request.getContent());
        }

        // 多模态消息：文本 + 图片（OpenAI Vision格式）
        List<Map<String, Object>> contentParts = new ArrayList<>();

        // 文本部分
        contentParts.add(Map.of("type", "text", "text", request.getContent()));

        // 图片部分
        for (String imageUrl : imageUrls) {
            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", imageUrl, "detail", "high")));
        }

        return Map.of("role", "user", "content", contentParts);
    }

    // ==================== 代码文件提取与合并 ====================

    /**
     * 从AI响应中提取结构化代码文件（通过 <<<AI_CODE_OUTPUT>>> 标记）
     * 如果没有找到标记，降级为正则提取
     */
    private String extractCodeFilesFromResponse(String responseText) {
        // 尝试结构化标记提取
        int startIdx = responseText.indexOf(CODE_OUTPUT_START);
        int endIdx = responseText.indexOf(CODE_OUTPUT_END);

        if (startIdx >= 0 && endIdx > startIdx) {
            String jsonBlock = responseText.substring(startIdx + CODE_OUTPUT_START.length(), endIdx).trim();
            try {
                // 验证JSON合法性
                List<Map<String, Object>> files = objectMapper.readValue(
                        jsonBlock, new TypeReference<List<Map<String, Object>>>() {
                        });
                // 过滤掉delete操作的文件，只保留create/modify
                List<Map<String, String>> result = files.stream()
                        .filter(f -> !"delete".equals(f.get("action")))
                        .map(f -> {
                            Map<String, String> m = new HashMap<>();
                            m.put("path", String.valueOf(f.get("path")));
                            m.put("language", String.valueOf(f.getOrDefault("language", "txt")));
                            m.put("content", String.valueOf(f.getOrDefault("content", "")));
                            m.put("action", String.valueOf(f.getOrDefault("action", "create")));
                            return m;
                        })
                        .collect(Collectors.toList());
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                log.warn("结构化代码标记解析失败，降级为正则提取: {}", e.getMessage());
            }
        }

        // 降级：正则提取代码块
        return extractCodeFilesByRegex(responseText);
    }

    /**
     * 合并AI输出到最新快照（增量模式）
     *
     * 逻辑：
     * - 旧快照中的文件保留
     * - AI输出action=create/modify的文件覆盖/新增
     * - AI输出action=delete的文件移除
     */
    private String mergeCodeFiles(String responseText, CodeSnapshot latestSnapshot) {
        String newFilesJson = extractCodeFilesFromResponse(responseText);
        if (newFilesJson == null) {
            // AI没有输出代码文件，保持旧快照不变
            return latestSnapshot != null ? latestSnapshot.getFiles() : null;
        }

        try {
            // 解析AI输出的文件列表
            List<Map<String, String>> newFiles = objectMapper.readValue(
                    newFilesJson, new TypeReference<List<Map<String, String>>>() {
                    });

            if (latestSnapshot == null || latestSnapshot.getFiles() == null) {
                // 没有旧快照，直接使用新文件
                return newFilesJson;
            }

            // 解析旧快照
            List<Map<String, String>> oldFiles = objectMapper.readValue(
                    latestSnapshot.getFiles(), new TypeReference<List<Map<String, String>>>() {
                    });

            // 用Map索引旧文件（path → file）
            Map<String, Map<String, String>> fileMap = new HashMap<>();
            for (Map<String, String> f : oldFiles) {
                fileMap.put(f.get("path"), f);
            }

            // 提取需要删除的文件路径
            int startIdx = responseText.indexOf(CODE_OUTPUT_START);
            int endIdx = responseText.indexOf(CODE_OUTPUT_END);
            if (startIdx >= 0 && endIdx > startIdx) {
                try {
                    String jsonBlock = responseText.substring(startIdx + CODE_OUTPUT_START.length(), endIdx).trim();
                    List<Map<String, Object>> rawFiles = objectMapper.readValue(
                            jsonBlock, new TypeReference<List<Map<String, Object>>>() {
                            });
                    for (Map<String, Object> rf : rawFiles) {
                        if ("delete".equals(rf.get("action"))) {
                            fileMap.remove(String.valueOf(rf.get("path")));
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // 覆盖/新增
            for (Map<String, String> nf : newFiles) {
                fileMap.put(nf.get("path"), nf);
            }

            return objectMapper.writeValueAsString(new ArrayList<>(fileMap.values()));
        } catch (Exception e) {
            log.warn("代码文件合并失败，使用新文件: {}", e.getMessage());
            return newFilesJson;
        }
    }

    /**
     * 提取本轮变更的文件路径列表
     */
    private List<String> extractChangedFilePaths(String responseText) {
        String filesJson = extractCodeFilesFromResponse(responseText);
        if (filesJson == null)
            return List.of();
        try {
            List<Map<String, String>> files = objectMapper.readValue(
                    filesJson, new TypeReference<List<Map<String, String>>>() {
                    });
            return files.stream().map(f -> f.get("path")).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    // ==================== 对话摘要 ====================

    /**
     * 异步检查并更新对话摘要（消息数超阈值时压缩早期历史）
     */
    private void updateSummaryIfNeeded(Conversation conversation) {
        try {
            List<Message> allMessages = conversationService.getMessages(conversation.getId());
            if (allMessages.size() <= SUMMARY_THRESHOLD) {
                return; // 消息数未超阈值，不需要摘要
            }

            // 取需要被压缩的早期消息（除了最近RECENT_MESSAGES_KEEP条）
            int compressEnd = allMessages.size() - RECENT_MESSAGES_KEEP;
            if (compressEnd <= 0)
                return;

            StringBuilder summaryInput = new StringBuilder();
            String existingSummary = conversation.getContextSummary();
            if (existingSummary != null && !existingSummary.isBlank()) {
                summaryInput.append("已有摘要：\n").append(existingSummary).append("\n\n");
            }
            summaryInput.append("新增对话内容：\n");
            for (int i = 0; i < compressEnd; i++) {
                Message msg = allMessages.get(i);
                if (msg.getContent() != null) {
                    String truncated = msg.getContent().length() > 500
                            ? msg.getContent().substring(0, 500) + "..."
                            : msg.getContent();
                    summaryInput.append("[").append(msg.getRole()).append("] ").append(truncated).append("\n");
                }
            }

            // 构建摘要（本地压缩，不调用大模型，避免额外Token消耗）
            String compressedSummary = buildLocalSummary(existingSummary, allMessages, compressEnd);
            conversationService.updateSummary(conversation.getId(), compressedSummary);

        } catch (Exception e) {
            log.warn("更新对话摘要失败: {}", e.getMessage());
        }
    }

    /**
     * 本地构建对话摘要（不调用大模型）
     * 提取每轮对话的关键意图和修改内容
     */
    private String buildLocalSummary(String existingSummary, List<Message> messages, int compressEnd) {
        StringBuilder sb = new StringBuilder();
        if (existingSummary != null && !existingSummary.isBlank()) {
            sb.append(existingSummary).append("\n");
        }

        for (int i = 0; i < compressEnd; i += 2) {
            Message msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                String userContent = msg.getContent();
                // 截取用户消息前100字作为意图摘要
                String intent = userContent.length() > 100
                        ? userContent.substring(0, 100) + "..."
                        : userContent;
                sb.append("- 用户要求: ").append(intent);

                // 如果下一条是assistant消息，提取修改说明（代码标记之前的文本）
                if (i + 1 < compressEnd) {
                    Message assistantMsg = messages.get(i + 1);
                    if ("assistant".equals(assistantMsg.getRole()) && assistantMsg.getContent() != null) {
                        String aiContent = assistantMsg.getContent();
                        int codeStart = aiContent.indexOf(CODE_OUTPUT_START);
                        String explanation = codeStart > 0
                                ? aiContent.substring(0, Math.min(codeStart, 200)).trim()
                                : (aiContent.length() > 200 ? aiContent.substring(0, 200) + "..." : aiContent);
                        sb.append(" → AI: ").append(explanation);
                    }
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 构建本轮对话的简要总结（用于前端展示）
     */
    private String buildTurnSummary(String userInput, List<String> changedFiles) {
        StringBuilder sb = new StringBuilder();
        String intent = userInput.length() > 80
                ? userInput.substring(0, 80) + "..."
                : userInput;
        sb.append("需求: ").append(intent);
        if (changedFiles != null && !changedFiles.isEmpty()) {
            sb.append(" | 变更文件: ").append(String.join(", ", changedFiles));
        }
        return sb.toString();
    }

    // ==================== 知识库注入 ====================

    private void appendKnowledgeContext(StringBuilder sb, List<Long> knowledgeBaseIds) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty())
            return;

        // 批量查询选中的知识库（个人/团队的不需要审核，公共的需要审核通过或平台类型）
        List<KnowledgeBase> selectedBases = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .in(KnowledgeBase::getId, knowledgeBaseIds)
                        .and(w -> w
                                // 公共/平台知识库需审核通过
                                .eq(KnowledgeBase::getReviewStatus, "approved")
                                .or().eq(KnowledgeBase::getType, "platform")
                                // 个人/团队知识库（非公共可见的 private 知识库可直接使用）
                                .or().eq(KnowledgeBase::getVisibility, "private")));

        if (selectedBases.isEmpty())
            return;

        List<Long> usedIds = selectedBases.stream()
                .map(KnowledgeBase::getId).collect(Collectors.toList());

        // 批量查询所有条目
        List<KnowledgeEntry> entries = knowledgeEntryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeEntry>()
                        .in(KnowledgeEntry::getKnowledgeBaseId, usedIds));

        if (!entries.isEmpty()) {
            sb.append("# 设计规范与组件库（必须遵循）\n\n");
            for (KnowledgeEntry entry : entries) {
                sb.append("## ").append(entry.getTitle()).append("\n");
                if (entry.getDescription() != null) {
                    sb.append(entry.getDescription()).append("\n");
                }
                if (entry.getCodeContent() != null) {
                    sb.append("```").append(entry.getCodeLanguage() != null ? entry.getCodeLanguage() : "")
                            .append("\n").append(entry.getCodeContent()).append("\n```\n\n");
                }
            }
        }

        // 批量累加使用次数
        knowledgeBaseService.incrementUsageCount(usedIds);
    }

    // ==================== 大模型API调用 ====================

    private void callModelApiWithRetry(ApiKey apiKey, List<Map<String, Object>> messages,
            SseEmitter emitter, StringBuilder fullContent, AtomicInteger totalTokens) {
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                callModelApi(apiKey, messages, emitter, fullContent, totalTokens);
                return;
            } catch (Exception e) {
                log.warn("模型API调用第{}次失败: {}", attempt, e.getMessage());
                if (attempt == retryCount) {
                    throw new BizException(ResultCode.FAIL.getCode(), "AI生成失败，请稍后重试: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 调用大模型API（OpenAI兼容格式，SSE流式，支持多模态）
     */
    private void callModelApi(ApiKey apiKey, List<Map<String, Object>> messages,
            SseEmitter emitter, StringBuilder fullContent, AtomicInteger totalTokens) {
        String baseUrl = apiKey.getApiBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = switch (apiKey.getProvider()) {
                case "openai" -> "https://api.openai.com/v1";
                case "anthropic" -> "https://api.anthropic.com/v1";
                case "deepseek" -> "https://api.deepseek.com/v1";
                default -> "https://api.openai.com/v1";
            };
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", apiKey.getModelName());
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        requestBody.put("max_tokens", 100000);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .responseTimeout(java.time.Duration.ofSeconds(timeoutSeconds));

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "Bearer " + apiKey.getApiKeyEncrypted())
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> {
                    String deltaContent = parseSseDelta(chunk);
                    if (deltaContent != null && !deltaContent.isEmpty()) {
                        fullContent.append(deltaContent);
                        sendSseEvent(emitter, SseEvent.builder()
                                .type("text_delta").content(deltaContent).build());
                    }
                    int tokens = parseSseUsage(chunk);
                    if (tokens > 0) {
                        totalTokens.set(tokens);
                    }
                })
                .doOnError(e -> log.error("模型API调用失败: {}", e.getMessage()))
                .blockLast();
    }

    // ==================== 工具方法 ====================

    /** 解析SSE流中的delta内容（OpenAI格式） */
    private String parseSseDelta(String chunk) {
        try {
            if ("[DONE]".equals(chunk.trim()))
                return null;
            String json = chunk.startsWith("data:") ? chunk.substring(5).trim() : chunk.trim();
            if (json.isEmpty() || "[DONE]".equals(json))
                return null;
            JsonNode root = objectMapper.readTree(json);
            JsonNode delta = root.path("choices").path(0).path("delta").path("content");
            return delta.isMissingNode() ? null : delta.asText();
        } catch (Exception e) {
            return null;
        }
    }

    /** 解析SSE流中的usage字段 */
    private int parseSseUsage(String chunk) {
        try {
            String json = chunk.startsWith("data:") ? chunk.substring(5).trim() : chunk.trim();
            if (json.isEmpty() || "[DONE]".equals(json))
                return 0;
            JsonNode root = objectMapper.readTree(json);
            JsonNode usage = root.path("usage").path("total_tokens");
            return usage.isMissingNode() ? 0 : usage.asInt();
        } catch (Exception e) {
            return 0;
        }
    }

    /** 降级方案：用正则提取代码块（当结构化标记解析失败时使用） */
    private String extractCodeFilesByRegex(String responseText) {
        try {
            List<Map<String, String>> files = new ArrayList<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?:// (\\S+)\\n)?```(\\w+)\\n([\\s\\S]*?)```", java.util.regex.Pattern.MULTILINE);
            java.util.regex.Matcher matcher = pattern.matcher(responseText);

            int fileIndex = 0;
            while (matcher.find()) {
                String filePath = matcher.group(1);
                String lang = matcher.group(2);
                String code = matcher.group(3);
                if (filePath == null || filePath.isBlank()) {
                    filePath = "src/file_" + (++fileIndex) + "." + langToExtension(lang);
                }
                files.add(Map.of("path", filePath, "language", lang, "content", code, "action", "create"));
            }
            return files.isEmpty() ? null : objectMapper.writeValueAsString(files);
        } catch (Exception e) {
            log.warn("正则提取代码文件失败", e);
            return null;
        }
    }

    private String langToExtension(String lang) {
        return switch (lang) {
            case "html" -> "html";
            case "tsx", "jsx" -> "tsx";
            case "css" -> "css";
            case "typescript", "ts" -> "ts";
            case "javascript", "js" -> "js";
            case "json" -> "json";
            default -> "txt";
        };
    }

    /** 构建附件JSON（图片+文件URL合并存储） */
    private String buildAttachmentsJson(GenerateRequest request) {
        try {
            Map<String, Object> attachments = new HashMap<>();
            if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
                attachments.put("images", request.getImageUrls());
            }
            if (request.getFileUrls() != null && !request.getFileUrls().isEmpty()) {
                attachments.put("files", request.getFileUrls());
            }
            return attachments.isEmpty() ? null : objectMapper.writeValueAsString(attachments);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendSseEvent(SseEmitter emitter, SseEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.getType())
                    .data(event, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.warn("发送SSE事件失败: {}", e.getMessage());
        }
    }
}