package com.chatsever.channel.service;

import com.chatsever.common.dto.ChannelDto;
import com.chatsever.channel.dto.ChannelRequest;
import java.util.List;

public interface ChannelService {
    ChannelDto createChannel(ChannelRequest request);
    List<ChannelDto> getChannelsByServerId(Long serverId);
    void deleteChannel(Long id);
    void deleteChannelsByServerId(Long serverId);
}
