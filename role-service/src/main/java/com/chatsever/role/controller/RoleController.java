package com.chatsever.role.controller;

import com.chatsever.role.model.Role;
import com.chatsever.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API cho role-service (R1-R6).
 * Base path: /api/servers/{serverId}/...
 */
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    // ================================================================
    // R1 — Tạo role
    // POST /api/servers/{serverId}/roles
    // ================================================================
    @PostMapping("/{serverId}/roles")
    public ResponseEntity<Role> createRole(
            @PathVariable String serverId,
            @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(roleService.createRole(
                serverId,
                payload.get("roleName"),
                payload.get("color"),
                payload.get("permissions")
        ));
    }

    // ================================================================
    // R1 (bonus) — Xem danh sách roles
    // GET /api/servers/{serverId}/roles
    // ================================================================
    @GetMapping("/{serverId}/roles")
    public ResponseEntity<List<Role>> getRoles(@PathVariable String serverId) {
        return ResponseEntity.ok(roleService.getRolesByServer(serverId));
    }

    // ================================================================
    // R1 (bonus) — Cập nhật role
    // PUT /api/servers/roles/{roleId}
    // ================================================================
    @PutMapping("/roles/{roleId}")
    public ResponseEntity<Role> updateRole(
            @PathVariable String roleId,
            @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(roleService.updateRole(
                roleId,
                payload.get("roleName"),
                payload.get("color"),
                payload.get("permissions")
        ));
    }

    // ================================================================
    // R1 (bonus) — Xóa role
    // DELETE /api/servers/roles/{roleId}
    // ================================================================
    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable String roleId) {
        return ResponseEntity.ok(Map.of("message", roleService.deleteRole(roleId)));
    }

    // ================================================================
    // R2 — Gán role cho member
    // PUT /api/servers/{serverId}/members/{userId}/roles
    // Body: { "roleIds": ["id1", "id2"] }
    // ================================================================
    @SuppressWarnings("unchecked")
    @PutMapping("/{serverId}/members/{userId}/roles")
    public ResponseEntity<Map<String, Object>> assignRoles(
            @PathVariable String serverId,
            @PathVariable String userId,
            @RequestBody Map<String, Object> payload) {
        List<String> roleIds = (List<String>) payload.get("roleIds");
        return ResponseEntity.ok(roleService.assignRoles(serverId, userId, roleIds));
    }

    // ================================================================
    // R4 — Kiểm tra effective permissions
    // GET /api/servers/{serverId}/permissions/{userId}
    // ================================================================
    @GetMapping("/{serverId}/permissions/{userId}")
    public ResponseEntity<Map<String, Object>> getPermissions(
            @PathVariable String serverId,
            @PathVariable String userId) {
        return ResponseEntity.ok(roleService.getEffectivePermissions(serverId, userId));
    }

    // ================================================================
    // R5 — Kick member
    // POST /api/servers/{serverId}/kick/{userId}
    // ================================================================
    @PostMapping("/{serverId}/kick/{userId}")
    public ResponseEntity<Map<String, String>> kickMember(
            @PathVariable String serverId,
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String requesterId) {
        return ResponseEntity.ok(roleService.kickMember(serverId, userId, requesterId));
    }

    // ================================================================
    // R6 — Ban member
    // POST /api/servers/{serverId}/ban/{userId}
    // ================================================================
    @PostMapping("/{serverId}/ban/{userId}")
    public ResponseEntity<Map<String, String>> banMember(
            @PathVariable String serverId,
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String requesterId,
            @RequestBody(required = false) Map<String, String> payload) {
        String reason = (payload != null) ? payload.get("reason") : null;
        return ResponseEntity.ok(roleService.banMember(serverId, userId, requesterId, reason));
    }

    // ================================================================
    // API nội bộ — Kiểm tra user có bị ban không
    // GET /api/servers/{serverId}/ban/{userId}/check
    // ================================================================
    @GetMapping("/{serverId}/ban/{userId}/check")
    public ResponseEntity<Map<String, Object>> checkBanned(
            @PathVariable String serverId,
            @PathVariable String userId) {
        boolean banned = roleService.isBanned(serverId, userId);
        return ResponseEntity.ok(Map.of("serverId", serverId, "userId", userId, "banned", banned));
    }

    // ================================================================
    // API nội bộ — Tạo default roles khi server mới được tạo
    // POST /api/servers/{serverId}/roles/init
    // ================================================================
    @PostMapping("/{serverId}/roles/init")
    public ResponseEntity<Map<String, String>> initDefaultRoles(@PathVariable String serverId) {
        roleService.createDefaultRoles(serverId);
        return ResponseEntity.ok(Map.of("message", "Đã tạo default roles cho server " + serverId));
    }
}