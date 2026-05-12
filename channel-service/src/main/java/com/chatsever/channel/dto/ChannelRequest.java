package com.chatsever.channel.dto;

import lombok.Data;

@Data
public class ChannelRequest {
    private String name;
    private Long serverId;
    private String type;
}
