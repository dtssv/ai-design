package com.aicopilot.api.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.aicopilot.api.service.ShareService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP (Model Context Protocol) 协议处理器
 * 实现 JSON-RPC 2.0 over Streamable HTTP（无状态模式）
 *
 * 全局唯一端点 /mcp，用户配置一次即可。
 * 通过 get_code 工具的 share_url 参数指定要获取的分享链接。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpProtocolHandler {

    private static final String JSONRPC_VERSION = "2.0";
    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final String SERVER_NAME = "ai-copilot-mcp";
    private static final String SERVER_VERSION = "1.0.0";

    /** 从分享链接中提取 token 的正则：/share/{token} 或 /mcp/{token} */
    private static final Pattern SHARE_TOKEN_PATTERN = Pattern.compile("/(?:share|mcp)/([a-zA-Z0-9]+)");

    private final ShareService shareService;
    private final ObjectMapper objectMapper;

    /**
     * 处理 JSON-RPC 2.0 请求
     * 
     * @param requestBody JSON-RPC 请求体
     * @param mcpToken    URL 路径中的用户 MCP 令牌，用于鉴权
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleRequest(Map<String, Object> requestBody, String mcpToken) {
        String jsonrpc = (String) requestBody.get("jsonrpc");
        String method = (String) requestBody.get("method");
        Object id = requestBody.get("id");
        Map<String, Object> params = (Map<String, Object>) requestBody.getOrDefault("params", Map.of());

        if (!JSONRPC_VERSION.equals(jsonrpc)) {
            return buildError(id, -32600, "Invalid JSON-RPC version, expected 2.0");
        }

        if (method == null || method.isBlank()) {
            return buildError(id, -32600, "Missing method");
        }

        // 通知类型消息（无 id）不需要响应
        if (id == null) {
            handleNotification(method, params);
            return null;
        }

        return switch (method) {
            case "initialize" -> handleInitialize(id);
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolsCall(id, params, mcpToken);
            default -> buildError(id, -32601, "Method not found: " + method);
        };
    }

    private Map<String, Object> handleInitialize(Object id) {
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of());

        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);

        return buildResult(id, result);
    }

    private Map<String, Object> handleToolsList(Object id) {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(buildGetCodeTool());

        Map<String, Object> result = new HashMap<>();
        result.put("tools", tools);
        return buildResult(id, result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> handleToolsCall(Object id, Map<String, Object> params, String mcpToken) {
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());

        if (!"get_code".equals(toolName)) {
            return buildError(id, -32602, "Unknown tool: " + toolName);
        }

        // 从 share_url 参数提取 token
        String shareUrl = (String) arguments.get("share_url");
        if (shareUrl == null || shareUrl.isBlank()) {
            return buildToolError(id, "缺少必填参数 share_url，请提供分享链接。");
        }

        String token = extractToken(shareUrl);
        if (token == null) {
            return buildToolError(id, "无法从链接中解析分享 token，请检查链接格式。");
        }

        try {
            Map<String, Object> codeResult;
            Object versionObj = arguments.get("version");

            if (versionObj != null) {
                int version;
                if (versionObj instanceof Number) {
                    version = ((Number) versionObj).intValue();
                } else {
                    version = Integer.parseInt(versionObj.toString());
                }
                codeResult = shareService.getMcpVersionCodeByMcpToken(mcpToken, token, version);
            } else {
                codeResult = shareService.getMcpLatestCodeByMcpToken(mcpToken, token);
            }

            String content = objectMapper.writeValueAsString(codeResult);

            List<Map<String, Object>> contentList = new ArrayList<>();
            contentList.add(Map.of("type", "text", "text", content));

            Map<String, Object> result = new HashMap<>();
            result.put("content", contentList);
            result.put("isError", false);
            return buildResult(id, result);

        } catch (Exception e) {
            log.warn("MCP tool call failed: {}", e.getMessage());
            return buildToolError(id, e.getMessage());
        }
    }

    /**
     * 从分享链接中提取 token
     * 支持格式:
     * - https://xxx/share/abc123
     * - https://xxx/mcp/abc123
     * - abc123 （直接传 token）
     */
    private String extractToken(String shareUrl) {
        Matcher matcher = SHARE_TOKEN_PATTERN.matcher(shareUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // 如果不含路径格式，当作纯 token 处理
        String trimmed = shareUrl.trim();
        if (trimmed.matches("^[a-zA-Z0-9]{16,64}$")) {
            return trimmed;
        }
        return null;
    }

    private void handleNotification(String method, Map<String, Object> params) {
        log.debug("MCP notification received: {}", method);
    }

    private Map<String, Object> buildGetCodeTool() {
        Map<String, Object> shareUrlProp = new HashMap<>();
        shareUrlProp.put("type", "string");
        shareUrlProp.put("description", "分享链接地址，如 https://example.com/share/abc123");

        Map<String, Object> versionProp = new HashMap<>();
        versionProp.put("type", "integer");
        versionProp.put("description", "版本号（可选），不指定则获取最新版本");

        Map<String, Object> properties = new HashMap<>();
        properties.put("share_url", shareUrlProp);
        properties.put("version", versionProp);

        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        inputSchema.put("required", List.of("share_url"));

        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "get_code");
        tool.put("description", "根据分享链接获取工作区的代码文件。用户提供分享链接后，返回包含文件路径、语言和内容的代码文件列表。可指定版本号获取特定版本，不指定则获取最新版本。");
        tool.put("inputSchema", inputSchema);
        return tool;
    }

    private Map<String, Object> buildResult(Object id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> buildError(Object id, int code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("error", error);
        return response;
    }

    private Map<String, Object> buildToolError(Object id, String message) {
        List<Map<String, Object>> contentList = new ArrayList<>();
        contentList.add(Map.of("type", "text", "text", "Error: " + message));

        Map<String, Object> result = new HashMap<>();
        result.put("content", contentList);
        result.put("isError", true);
        return buildResult(id, result);
    }
}