package com.chatsever.channel.service.impl;

import com.chatsever.channel.model.Channel;
import com.chatsever.channel.model.ChannelType;
import com.chatsever.channel.repository.ChannelRepository;
import com.chatsever.channel.service.ChannelService;
import com.chatsever.common.dto.ChannelDto;
import com.chatsever.channel.dto.ChannelRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {
    
    private final ChannelRepository channelRepository;

    @Override
    @Transactional
    public ChannelDto createChannel(ChannelRequest request) {
        Channel channel = Channel.builder()
                .name(request.getName())
                .serverId(request.getServerId())
                .type(ChannelType.valueOf(request.getType().toUpperCase()))
                .build();
        channel = channelRepository.save(channel);
        return mapToDto(channel);
    }

    @Override
    public List<ChannelDto> getChannelsByServerId(Long serverId) {
        return channelRepository.findByServerId(serverId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteChannel(Long id) {
        channelRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteChannelsByServerId(Long serverId) {
        channelRepository.deleteByServerId(serverId);
    }

    private ChannelDto mapToDto(Channel channel) {
        return ChannelDto.builder()
                .id(channel.getId())
                .name(channel.getName())
                .serverId(channel.getServerId())
                .type(channel.getType().name())
                .build();
    }
}
