package network.dto;

import java.time.LocalDateTime;

public class ServerDto {
    private Long id;
    private String name;
    private String description;
    private String ownerId;
    private String inviteCode;
    private LocalDateTime createdAt;

    public ServerDto() {
    }

    public ServerDto(Long id, String name, String description, String ownerId, String inviteCode, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
