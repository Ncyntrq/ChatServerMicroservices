package com.chatsever.channel.service;

import com.chatsever.common.dto.ChannelDto;
import com.chatsever.channel.dto.ChannelRequest;
import com.chatsever.channel.model.PinnedMessage;

import java.util.List;

public interface ChannelService {
    ChannelDto createChannel(ChannelRequest request, String userId);
    List<ChannelDto> getChannelsByServerId(Long serverId);
    ChannelDto updateChannel(Long id, ChannelRequest request, String userId); // CH3
    void deleteChannel(Long id, String userId);
    void deleteChannelsByServerId(Long serverId);

    // CH6, CH7 — Pin messages
    PinnedMessage pinMessage(Long channelId, Long messageId, String pinnedBy);
    void unpinMessage(Long channelId, Long messageId, String userId);
    List<PinnedMessage> getPinnedMessages(Long channelId);

    // CH8 — Pin channels
    ChannelDto togglePinChannel(Long channelId, String userId);
}
