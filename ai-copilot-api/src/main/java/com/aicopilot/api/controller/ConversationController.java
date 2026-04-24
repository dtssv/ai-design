package com.aicopilot.api.controller;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aicopilot.api.common.exception.BizException;
import com.aicopilot.api.common.result.R;
import com.aicopilot.api.common.result.ResultCode;
import com.aicopilot.api.common.security.SecurityUtil;
import com.aicopilot.api.dto.ai.GenerateRequest;
import com.aicopilot.api.entity.ApiKey;
import com.aicopilot.api.entity.CodeSnapshot;
import com.aicopilot.api.entity.Conversation;
import com.aicopilot.api.entity.Message;
import com.aicopilot.api.service.AiGenerationService;
import com.aicopilot.api.service.ApiKeyPoolService;
import com.aicopilot.api.service.ConversationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final AiGenerationService aiGenerationService;
    private final ApiKeyPoolService apiKeyPoolService;
    private final ObjectMapper objectMapper;

    @PostMapping("/workspaces/{wid}/conversations")
    public R<Conversation> create(@PathVariable Long wid, @RequestBody Map<String, String> body) {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.ok(conversationService.createConversation(
                wid, body.get("title"), body.get("generation_mode"), userId));
    }

    @GetMapping("/workspaces/{wid}/conversations")
    public R<List<Conversation>> list(@PathVariable Long wid) {
        return R.ok(conversationService.listByWorkspace(wid));
    }

    @GetMapping("/conversations/{id}/messages")
    public R<List<Message>> messages(@PathVariable Long id) {
        return R.ok(conversationService.getMessages(id));
    }

    @PostMapping("/conversations/{id}/generate")
    public SseEmitter generate(@PathVariable Long id, @RequestBody GenerateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return aiGenerationService.generateStream(id, request, userId);
    }

    @PutMapping("/conversations/{id}/mode")
    public R<Void> switchMode(@PathVariable Long id, @RequestBody Map<String, String> body) {
        conversationService.switchMode(id, body.get("generation_mode"));
        return R.ok();
    }

    @PutMapping("/conversations/{id}/api-key")
    public R<Void> switchApiKey(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String apiKeySource = (String) body.get("api_key_source");
        Long apiKeyId = body.get("api_key_id") != null
                ? Long.valueOf(body.get("api_key_id").toString())
                : null;
        conversationService.switchApiKeySource(id, apiKeySource, apiKeyId);
        return R.ok();
    }

    @PostMapping("/conversations/{id}/upgrade")
    public SseEmitter upgrade(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        return aiGenerationService.upgradeToDevMode(id, userId);
    }

    /** 获取代码版本列表（不含files内容，减少传输量） */
    @GetMapping("/conversations/{id}/snapshots")
    public R<List<Map<String, Object>>> snapshots(@PathVariable Long id) {
        List<CodeSnapshot> list = conversationService.getSnapshots(id);
        List<Map<String, Object>> result = list.stream().map(s -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", s.getId());
            m.put("version", s.getVersion());
            m.put("generationMode", s.getGenerationMode());
            m.put("createdAt", s.getCreatedAt());
            // 只返回文件路径摘要，不返回content
            m.put("fileCount", countFiles(s.getFiles()));
            return m;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    /** 获取单个快照详情（含完整files内容） */
    @GetMapping("/snapshots/{snapshotId}")
    public R<CodeSnapshot> snapshotDetail(@PathVariable Long snapshotId) {
        CodeSnapshot snapshot = conversationService.getSnapshotById(snapshotId);
        if (snapshot == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return R.ok(snapshot);
    }

    /** 打包下载快照代码为ZIP（保持路径和分层） */
    @GetMapping("/snapshots/{snapshotId}/download")
    public ResponseEntity<byte[]> downloadSnapshot(@PathVariable Long snapshotId) {
        CodeSnapshot snapshot = conversationService.getSnapshotById(snapshotId);
        if (snapshot == null || snapshot.getFiles() == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }

        try {
            List<Map<String, String>> files = objectMapper.readValue(
                    snapshot.getFiles(), new TypeReference<List<Map<String, String>>>() {
                    });

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                for (Map<String, String> file : files) {
                    String path = file.getOrDefault("path", "unknown.txt");
                    String content = file.getOrDefault("content", "");
                    ZipEntry entry = new ZipEntry(path);
                    zos.putNextEntry(entry);
                    zos.write(content.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }

            String fileName = "code-v" + snapshot.getVersion() + ".zip";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(baos.toByteArray());
        } catch (Exception e) {
            throw new BizException(ResultCode.FAIL.getCode(), "打包下载失败: " + e.getMessage());
        }
    }

    @GetMapping("/models/available")
    public R<List<Map<String, Object>>> availableModels() {
        List<ApiKey> models = apiKeyPoolService.listAvailableModels();
        List<Map<String, Object>> result = models.stream()
                .map(k -> Map.<String, Object>of(
                        "provider", k.getProvider(),
                        "model_name", k.getModelName()))
                .collect(Collectors.toList());
        return R.ok(result);
    }

    private int countFiles(String filesJson) {
        if (filesJson == null || filesJson.isBlank())
            return 0;
        try {
            List<?> list = objectMapper.readValue(filesJson, List.class);
            return list.size();
        } catch (Exception e) {
            return 0;
        }
    }
}