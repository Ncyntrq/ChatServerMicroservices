package com.chatsever.role.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu danh sách user bị ban khỏi server (R6).
 */
@Entity
@Table(name = "banned_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"serverId", "userId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BannedMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String serverId;

    @Column(nullable = false)
    private String userId;

    /** Người thực hiện ban */
    private String bannedBy;

    /** Lý do ban */
    private String reason;

    private LocalDateTime bannedAt;

    @PrePersist
    protected void onCreate() { bannedAt = LocalDateTime.now(); }
}
