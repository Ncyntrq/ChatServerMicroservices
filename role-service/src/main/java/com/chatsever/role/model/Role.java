package com.chatsever.role.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity vai trò trong server (R1).
 * Mỗi server có nhiều roles, mỗi role có bitmask permissions.
 */
@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String serverId;

    @Column(nullable = false)
    private String roleName;

    private String color;

    /** Bitmask permissions (R3) */
    @Column(nullable = false)
    private int permissionBitmask;

    /** Cờ đánh dấu role mặc định (Owner/Admin/Moderator/Member) — không xóa được */
    @Builder.Default
    private boolean isDefault = false;

    /** Thứ tự hiển thị (Owner cao nhất) */
    @Builder.Default
    private int priority = 0;
}