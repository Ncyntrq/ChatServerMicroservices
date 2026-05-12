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
public class ServerDto {
    private Long id;
    private String name;
    private String description;
    private String ownerId;
    private String inviteCode;
    private LocalDateTime createdAt;
}
