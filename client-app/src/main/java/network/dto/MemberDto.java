package network.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MemberDto {
    private Long id;
    private String userId;
    private Long serverId;
    // Đã thay thế Enum bằng List<Long> để lưu ID quyền từ role-service
    private List<Long> roleIds;
    private LocalDateTime joinedAt;

    public MemberDto() {
    }

    public MemberDto(Long id, String userId, Long serverId, List<Long> roleIds, LocalDateTime joinedAt) {
        this.id = id;
        this.userId = userId;
        this.serverId = serverId;
        this.roleIds = roleIds;
        this.joinedAt = joinedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
