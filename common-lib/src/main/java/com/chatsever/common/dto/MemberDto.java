package com.chatsever.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
    private Long id;
    private String userId;
    private Long serverId;
    // Đã thay thế Enum bằng List<Long> để lưu ID quyền từ role-service
    private List<Long> roleIds;
    private LocalDateTime joinedAt;
}
