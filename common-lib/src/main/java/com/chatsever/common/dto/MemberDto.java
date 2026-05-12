package com.chatsever.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
    private Long id;
    private String userId;
    private Long serverId;
    private String role; // OWNER, ADMIN, MEMBER
    private LocalDateTime joinedAt;
}
