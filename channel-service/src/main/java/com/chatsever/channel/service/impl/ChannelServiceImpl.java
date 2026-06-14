package com.chatsever.channel.service.impl;

import com.chatsever.channel.model.Channel;
import com.chatsever.channel.model.ChannelType;
import com.chatsever.channel.model.PinnedMessage;
import com.chatsever.channel.repository.ChannelRepository;
import com.chatsever.channel.repository.PinnedMessageRepository;
import com.chatsever.channel.service.ChannelService;
import com.chatsever.common.dto.ChannelDto;
import com.chatsever.channel.dto.ChannelRequest;
import com.chatsever.channel.client.RoleClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final PinnedMessageRepository pinnedMessageRepository;
    private final RoleClient roleClient;

    @Override
    @Transactional
    public ChannelDto createChannel(ChannelRequest request, String userId) {
        if (!"system".equals(userId)) {
            checkPermission(request.getServerId(), userId, 8); // MANAGE_CHANNEL
        }
        
        Channel channel = Channel.builder()
                .name(request.getName())
                .serverId(request.getServerId())
                .type(ChannelType.valueOf(request.getType().toUpperCase()))
                .topic(request.getTopic())
                .slowmode(request.getSlowmode() != null ? request.getSlowmode() : 0)
                .category(request.getCategory())
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

    // CH3 — Cập nhật channel (đổi tên, topic, slowmode, category)
    @Override
    @Transactional
    public ChannelDto updateChannel(Long id, ChannelRequest request, String userId) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Channel not found: " + id));

        checkPermission(channel.getServerId(), userId, 8); // MANAGE_CHANNEL

        if (request.getName() != null) channel.setName(request.getName());
        if (request.getTopic() != null) channel.setTopic(request.getTopic());
        if (request.getSlowmode() != null) channel.setSlowmode(request.getSlowmode());
        if (request.getCategory() != null) channel.setCategory(request.getCategory());

        channel = channelRepository.save(channel);
        return mapToDto(channel);
    }

    @Override
    @Transactional
    public void deleteChannel(Long id, String userId) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Channel not found: " + id));
        checkPermission(channel.getServerId(), userId, 8); // MANAGE_CHANNEL
        channelRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteChannelsByServerId(Long serverId) {
        channelRepository.deleteByServerId(serverId);
    }

    // CH6 — Ghim tin nhắn
    @Override
    @Transactional
    public PinnedMessage pinMessage(Long channelId, Long messageId, String pinnedBy) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found: " + channelId));
        checkPermission(channel.getServerId(), pinnedBy, 4); // MANAGE_MESSAGES
        
        // Check if already pinned
        if (pinnedMessageRepository.findByChannelIdAndMessageId(channelId, messageId).isPresent()) {
            throw new RuntimeException("Tin nhắn đã được ghim");
        }
        return pinnedMessageRepository.save(new PinnedMessage(channelId, messageId, pinnedBy));
    }

    // CH6 — Bỏ ghim
    @Override
    @Transactional
    public void unpinMessage(Long channelId, Long messageId, String userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found: " + channelId));
        checkPermission(channel.getServerId(), userId, 4); // MANAGE_MESSAGES
        
        pinnedMessageRepository.deleteByChannelIdAndMessageId(channelId, messageId);
    }

    // CH7 — Danh sách tin nhắn đã ghim
    @Override
    public List<PinnedMessage> getPinnedMessages(Long channelId) {
        return pinnedMessageRepository.findByChannelIdOrderByPinnedAtDesc(channelId);
    }

    // CH8 — Ghim kênh (Channel)
    @Override
    @Transactional
    public ChannelDto togglePinChannel(Long channelId, String userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found: " + channelId));

        checkPermission(channel.getServerId(), userId, 8); // MANAGE_CHANNEL

        if (channel.getPinnedAt() != null) {
            channel.setPinnedAt(null);
        } else {
            channel.setPinnedAt(System.currentTimeMillis());
        }

        channel = channelRepository.save(channel);
        return mapToDto(channel);
    }

    private ChannelDto mapToDto(Channel channel) {
        return ChannelDto.builder()
                .id(channel.getId())
                .name(channel.getName())
                .serverId(channel.getServerId())
                .type(channel.getType().name())
                .topic(channel.getTopic())
                .slowmode(channel.getSlowmode())
                .category(channel.getCategory())
                .pinnedAt(channel.getPinnedAt())
                .build();
    }

    private void checkPermission(Long serverId, String userId, int requiredPermissionBit) {
        try {
            java.util.Map<String, Object> perms = roleClient.getPermissions(serverId, userId);
            if (perms != null && perms.containsKey("permissionBitmask")) {
                int bitmask = (int) perms.get("permissionBitmask");
                // Check nếu có quyền tương ứng, HOẶC có quyền ADMIN (128), HOẶC OWNER (255)
                if ((bitmask & requiredPermissionBit) != 0 || (bitmask & 128) != 0 || bitmask == 255) {
                    return; // Có quyền
                }
            }
            throw new RuntimeException("Bạn không có đủ quyền (cần " + requiredPermissionBit + " hoặc ADMIN)");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kiểm tra phân quyền: " + e.getMessage());
        }
    }
}
