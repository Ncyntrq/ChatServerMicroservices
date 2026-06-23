package com.chatsever.role.service;

import com.chatsever.grpc.server.*;
import net.devh.boot.grpc.client.inject.GrpcClient;

import com.chatsever.role.model.BannedMember;
import com.chatsever.role.model.Permission;
import com.chatsever.role.model.Role;
import com.chatsever.role.repository.BannedMemberRepository;
import com.chatsever.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.chatsever.role.adapter.ServerInfoGrpcAdapter;

import java.util.List;
import java.util.Map;

/**
 * Service xử lý vai trò & phân quyền (R1-R6).
 * Giao tiếp với server-service qua gRPC để quản lý members.
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final BannedMemberRepository bannedMemberRepository;

    @Autowired
    private ServerInfoGrpcAdapter serverServiceClient;

    // ========================================================================
    // R1 — Tạo role
    // ========================================================================
    @Transactional
    public Role createRole(String serverId, String roleName, String color, String permissions) {
        if (roleRepository.findByServerIdAndRoleName(serverId, roleName).isPresent()) {
            throw new RuntimeException("Role '" + roleName + "' đã tồn tại trong server này");
        }

        int bitmask = Permission.fromNames(permissions);
        Role role = Role.builder()
                .serverId(serverId)
                .roleName(roleName)
                .color(color)
                .permissionBitmask(bitmask)
                .isDefault(false)
                .priority(0)
                .build();
        Role saved = roleRepository.save(role);
        log.info("Created role: serverId={}, name={}, permissions={}", serverId, roleName, bitmask);
        return saved;
    }

    // ========================================================================
    // R1 (bonus) — Xem danh sách roles của server
    // ========================================================================
    public List<Role> getRolesByServer(String serverId) {
        return roleRepository.findByServerIdOrderByPriorityDesc(serverId);
    }

    // ========================================================================
    // R1 (bonus) — Cập nhật role
    // ========================================================================
    @Transactional
    public Role updateRole(String roleId, String roleName, String color, String permissions) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role: " + roleId));
        if (role.isDefault()) {
            throw new RuntimeException("Không thể sửa role mặc định: " + role.getRoleName());
        }
        if (roleName != null && !roleName.isBlank()) {
            role.setRoleName(roleName);
        }
        if (color != null) {
            role.setColor(color);
        }
        if (permissions != null) {
            role.setPermissionBitmask(Permission.fromNames(permissions));
        }
        Role saved = roleRepository.save(role);
        log.info("Updated role: id={}, name={}", roleId, role.getRoleName());
        return saved;
    }

    // ========================================================================
    // R1 (bonus) — Xóa role (chỉ custom roles, không xóa default)
    // ========================================================================
    @Transactional
    public String deleteRole(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role: " + roleId));
        if (role.isDefault()) {
            throw new RuntimeException("Không thể xóa role mặc định: " + role.getRoleName());
        }
        roleRepository.delete(role);
        log.info("Deleted role: id={}, name={}", roleId, role.getRoleName());
        return "Xóa vai trò thành công!";
    }

    // ========================================================================
    // R2 — Gán role cho member
    // PUT /api/servers/{serverId}/members/{userId}/roles
    // Body: { "roleIds": ["id1", "id2"] }
    // ========================================================================
    @Transactional
    public Map<String, Object> assignRoles(String serverId, String userId, List<String> roleIds) {
        List<Role> roles = roleRepository.findByIdIn(roleIds);
        for (Role r : roles) {
            if (!r.getServerId().equals(serverId)) {
                throw new RuntimeException("Role " + r.getId() + " không thuộc server " + serverId);
            }
        }

        try {
            UpdateMemberRolesRequest request = UpdateMemberRolesRequest.newBuilder()
                    .setServerId(Long.parseLong(serverId))
                    .setUserId(userId)
                    .addAllRoleIds(roleIds)
                    .build();
            serverServiceClient.updateMemberRoles(request);
        } catch (Exception e) {
            log.error("Lỗi gán role cho member: {}", e.getMessage());
            throw new RuntimeException("Không thể gán role: " + e.getMessage());
        }

        log.info("Assigned roles {} to user {} in server {}", roleIds, userId, serverId);
        return Map.of(
                "serverId", serverId,
                "userId", userId,
                "roleIds", roleIds,
                "message", "Gán vai trò thành công"
        );
    }

    // ========================================================================
    // R4 — Kiểm tra effective permissions
    // GET /api/servers/{serverId}/permissions/{userId}
    // ========================================================================
    public Map<String, Object> getEffectivePermissions(String serverId, String userId) {
        try {
            GetServerDetailsRequest request = GetServerDetailsRequest.newBuilder()
                    .setServerId(Long.parseLong(serverId))
                    .build();
            GetServerDetailsResponse response = serverServiceClient.getServerDetails(request);

            if (response.hasServer()) {
                if (userId.equals(response.getServer().getOwnerId())) {
                    return Map.of(
                            "userId", userId,
                            "serverId", serverId,
                            "role", "Owner",
                            "permissionBitmask", Permission.ALL,
                            "permissions", Permission.toNames(Permission.ALL)
                    );
                }

                for (MemberDetails member : response.getMembersList()) {
                    if (userId.equals(member.getUserId())) {
                        List<String> roleIds = member.getRoleIdsList();
                        if (roleIds == null || roleIds.isEmpty()) {
                            return Map.of(
                                    "userId", userId,
                                    "serverId", serverId,
                                    "role", "Member",
                                    "permissionBitmask", Permission.MEMBER_DEFAULT,
                                    "permissions", Permission.toNames(Permission.MEMBER_DEFAULT)
                            );
                        }

                        List<Role> roles = roleRepository.findByIdIn(roleIds);
                        int effectiveMask = 0;
                        String highestRole = "Member";
                        int highestPriority = -1;
                        for (Role r : roles) {
                            effectiveMask |= r.getPermissionBitmask();
                            if (r.getPriority() > highestPriority) {
                                highestPriority = r.getPriority();
                                highestRole = r.getRoleName();
                            }
                        }

                        return Map.of(
                                "userId", userId,
                                "serverId", serverId,
                                "role", highestRole,
                                "permissionBitmask", effectiveMask,
                                "permissions", Permission.toNames(effectiveMask)
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi kiểm tra quyền: {}", e.getMessage());
            throw new RuntimeException("Không thể kiểm tra quyền: " + e.getMessage());
        }

        throw new RuntimeException("User " + userId + " không phải member của server " + serverId);
    }

    // ========================================================================
    // R5 — Kick member
    // ========================================================================
    @Transactional
    public Map<String, String> kickMember(String serverId, String userId, String requesterId) {
        checkPermission(serverId, requesterId, Permission.KICK_MEMBER);

        if (userId.equals(requesterId)) {
            throw new RuntimeException("Không thể kick chính mình");
        }

        checkNotOwner(serverId, userId);

        try {
            LeaveServerRequest request = LeaveServerRequest.newBuilder()
                    .setServerId(Long.parseLong(serverId))
                    .setUserId(userId)
                    .build();
            serverServiceClient.leaveServer(request);
        } catch (Exception e) {
            log.error("Lỗi kick member: {}", e.getMessage());
            throw new RuntimeException("Không thể kick member: " + e.getMessage());
        }

        log.info("Kicked user {} from server {} by {}", userId, serverId, requesterId);
        return Map.of("message", "Đã kick " + userId + " khỏi server");
    }

    // ========================================================================
    // R6 — Ban member (vĩnh viễn + không cho join lại)
    // ========================================================================
    @Transactional
    public Map<String, String> banMember(String serverId, String userId, String requesterId, String reason) {
        checkPermission(serverId, requesterId, Permission.BAN_MEMBER);

        if (userId.equals(requesterId)) {
            throw new RuntimeException("Không thể ban chính mình");
        }

        checkNotOwner(serverId, userId);

        if (bannedMemberRepository.existsByServerIdAndUserId(serverId, userId)) {
            throw new RuntimeException("User " + userId + " đã bị ban trong server này");
        }

        BannedMember banned = BannedMember.builder()
                .serverId(serverId)
                .userId(userId)
                .bannedBy(requesterId)
                .reason(reason)
                .build();
        bannedMemberRepository.save(banned);

        try {
            LeaveServerRequest request = LeaveServerRequest.newBuilder()
                    .setServerId(Long.parseLong(serverId))
                    .setUserId(userId)
                    .build();
            serverServiceClient.leaveServer(request);
        } catch (Exception e) {
            log.warn("User {} có thể đã rời server {}: {}", userId, serverId, e.getMessage());
        }

        log.info("Banned user {} from server {} by {} (reason: {})", userId, serverId, requesterId, reason);
        return Map.of("message", "Đã ban " + userId + " khỏi server vĩnh viễn");
    }

    // ========================================================================
    // Kiểm tra user có bị ban không (dùng bởi server-service khi join)
    // ========================================================================
    public boolean isBanned(String serverId, String userId) {
        return bannedMemberRepository.existsByServerIdAndUserId(serverId, userId);
    }

    // ========================================================================
    // Tạo default roles khi server mới được tạo
    // ========================================================================
    @Transactional
    public void createDefaultRoles(String serverId) {
        roleRepository.save(Role.builder()
                .serverId(serverId).roleName("Owner")
                .color("#FFD700").permissionBitmask(Permission.ALL)
                .isDefault(true).priority(100).build());

        roleRepository.save(Role.builder()
                .serverId(serverId).roleName("Admin")
                .color("#FF4500").permissionBitmask(Permission.ADMIN_DEFAULT)
                .isDefault(true).priority(80).build());

        roleRepository.save(Role.builder()
                .serverId(serverId).roleName("Moderator")
                .color("#2ECC71").permissionBitmask(Permission.MODERATOR_DEFAULT)
                .isDefault(true).priority(50).build());

        roleRepository.save(Role.builder()
                .serverId(serverId).roleName("Member")
                .color("#95A5A6").permissionBitmask(Permission.MEMBER_DEFAULT)
                .isDefault(true).priority(10).build());

        log.info("Created default roles for server: {} (bitmask schema v2)", serverId);
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    private void checkPermission(String serverId, String requesterId, Permission requiredPermission) {
        Map<String, Object> perms = getEffectivePermissions(serverId, requesterId);
        int bitmask = (int) perms.get("permissionBitmask");
        if (!Permission.hasPermission(bitmask, requiredPermission)) {
            throw new RuntimeException("Bạn không có quyền " + requiredPermission.name() + " trong server này");
        }
    }

    private void checkNotOwner(String serverId, String userId) {
        try {
            GetServerDetailsRequest request = GetServerDetailsRequest.newBuilder()
                    .setServerId(Long.parseLong(serverId))
                    .build();
            GetServerDetailsResponse response = serverServiceClient.getServerDetails(request);
            if (response.hasServer() && userId.equals(response.getServer().getOwnerId())) {
                throw new RuntimeException("Không thể kick/ban Owner của server");
            }
        } catch (RuntimeException e) {
            throw e; 
        } catch (Exception e) {
            log.error("Lỗi kiểm tra owner: {}", e.getMessage());
        }
    }
}