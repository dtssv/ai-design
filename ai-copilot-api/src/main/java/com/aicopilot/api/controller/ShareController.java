package com.aicopilot.api.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.aicopilot.api.common.result.R;
import com.aicopilot.api.common.security.SecurityUtil;
import com.aicopilot.api.mcp.McpProtocolHandler;
import com.aicopilot.api.service.ShareService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;
    private final McpProtocolHandler mcpProtocolHandler;

    /**
     * 创建分享链接（需要登录）
     * POST /workspaces/{id}/share
     */
    @PostMapping("/workspaces/{id}/share")
    public R<Map<String, Object>> createShare(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long snapshotId = null;
        if (body != null && body.get("snapshotId") != null) {
            snapshotId = Long.valueOf(body.get("snapshotId").toString());
        }
        return R.ok(shareService.createShare(id, snapshotId, userId));
    }

    /**
     * 通过分享令牌获取预览数据（公开接口，无需登录）
     * GET /share/{token}
     */
    @GetMapping("/share/{token}")
    public R<Map<String, Object>> getSharePreview(@PathVariable String token) {
        return R.ok(shareService.getSharePreviewData(token));
    }

    // ========== 标准 MCP 协议端点 (Streamable HTTP) ==========

    /**
     * MCP Streamable HTTP 全局端点（公开接口，无需登录）
     * POST /mcp
     *
     * 用户只需配置一次此端点，通过 get_code 工具的 share_url 参数指定分享链接。
     * 接收 JSON-RPC 2.0 请求，支持:
     * - initialize: 协议握手
     * - notifications/initialized: 初始化完成通知
     * - tools/list: 列出可用工具 (get_code)
     * - tools/call: 调用工具获取代码（需传入 share_url）
     */
    @PostMapping(value = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> mcpEndpoint(
            @RequestBody Map<String, Object> body) {
        log.debug("MCP request received, method: {}", body.get("method"));

        Map<String, Object> response = mcpProtocolHandler.handleRequest(body);

        if (response == null) {
            return ResponseEntity.accepted().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // ========== 兼容旧版 REST 接口（后续可移除）==========

    /**
     * MCP 工具 - 获取最新版本代码（公开接口）
     * GET /mcp/{token}/latest
     */
    @GetMapping("/mcp/{token}/latest")
    public R<Map<String, Object>> mcpLatest(@PathVariable String token) {
        return R.ok(shareService.getMcpLatestCode(token));
    }

    /**
     * MCP 工具 - 获取指定版本代码（公开接口）
     * GET /mcp/{token}/version/{version}
     */
    @GetMapping("/mcp/{token}/version/{version}")
    public R<Map<String, Object>> mcpVersion(@PathVariable String token, @PathVariable int version) {
        return R.ok(shareService.getMcpVersionCode(token, version));
    }
}