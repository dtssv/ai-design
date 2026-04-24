package com.aicopilot.api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicopilot.api.common.result.R;
import com.aicopilot.api.common.security.SecurityUtil;
import com.aicopilot.api.entity.TeamMember;
import com.aicopilot.api.entity.Workspace;
import com.aicopilot.api.entity.WorkspaceFile;
import com.aicopilot.api.entity.WorkspaceMember;
import com.aicopilot.api.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public R<Workspace> create(@RequestBody Workspace workspace) {
        return R.ok(workspaceService.createWorkspace(workspace, SecurityUtil.getCurrentUserId()));
    }

    @GetMapping
    public R<List<Workspace>> list() {
        return R.ok(workspaceService.getMyWorkspaces(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public R<Workspace> detail(@PathVariable Long id) {
        return R.ok(workspaceService.getWorkspaceDetail(id, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Workspace workspace) {
        workspace.setId(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        return R.ok();
    }

    @GetMapping("/{id}/available-members")
    public R<List<TeamMember>> availableMembers(@PathVariable Long id) {
        return R.ok(workspaceService.getAvailableMembers(id));
    }

    @PostMapping("/{id}/members")
    public R<Void> addMember(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("user_id").toString());
        String permission = body.getOrDefault("permission", "view").toString();
        workspaceService.addMember(id, userId, permission, SecurityUtil.getCurrentUserId());
        return R.ok();
    }

    @PutMapping("/{id}/members/{userId}")
    public R<Void> updateMemberPermission(@PathVariable Long id, @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        return R.ok();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public R<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        return R.ok();
    }

    @GetMapping("/{id}/members")
    public R<List<WorkspaceMember>> members(@PathVariable Long id) {
        return R.ok(workspaceService.getMembers(id));
    }

    @GetMapping("/{id}/files")
    public R<List<WorkspaceFile>> files(@PathVariable Long id) {
        return R.ok(workspaceService.getFiles(id));
    }
}