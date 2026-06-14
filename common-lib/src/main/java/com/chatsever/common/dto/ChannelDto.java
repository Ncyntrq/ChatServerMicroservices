package com.chatsever.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDto {
    private Long id;
    private String name;
    private Long serverId;
    private String type; // TEXT, VOICE
    // CH3 fields
    private String topic;
    private Integer slowmode;
    // CH5 field
    private String category;
    // CH8 field
    private Long pinnedAt;
}
