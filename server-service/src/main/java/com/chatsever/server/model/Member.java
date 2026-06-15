package com.chatsever.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private Long serverId;

    // Hibernate sẽ tự tạo bảng "member_role_ids" để lưu danh sách ID này
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_role_ids", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "role_id")
    @Builder.Default
    private List<String> roleIds = new ArrayList<>();

    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() { joinedAt = LocalDateTime.now(); }
}