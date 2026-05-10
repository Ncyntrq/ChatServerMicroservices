package com.chatsever.server.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "servers")
public class Server {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String ownerId; // Chứa ID của người tạo (Được Gateway tự động truyền xuống)

    @Column(unique = true)
    private String inviteCode; // Mã mời để người khác tham gia nhóm

    private LocalDateTime createdAt;

    // Tự động sinh thời gian và mã mời ngẫu nhiên 8 ký tự khi tạo mới
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.inviteCode = UUID.randomUUID().toString().substring(0, 8);
    }

    // --- GETTER & SETTER ---
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