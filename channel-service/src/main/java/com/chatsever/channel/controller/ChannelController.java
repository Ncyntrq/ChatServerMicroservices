package com.chatsever.channel.controller;

import com.chatsever.channel.dto.ChannelRequest;
import com.chatsever.channel.model.PinnedMessage;
import com.chatsever.channel.service.ChannelService;
import com.chatsever.common.dto.ChannelDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    // CH1 — Tạo channel
    @PostMapping
    public ResponseEntity<ChannelDto> createChannel(
            @RequestBody ChannelRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return ResponseEntity.ok(channelService.createChannel(request, userId));
    }

    // CH2 — Xem danh sách channels của server
    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<ChannelDto>> getChannelsByServerId(@PathVariable Long serverId) {
        return ResponseEntity.ok(channelService.getChannelsByServerId(serverId));
    }

    // CH3 — Cập nhật channel (đổi tên, topic, slowmode, category)
    @PutMapping("/{channelId}")
    public ResponseEntity<ChannelDto> updateChannel(
            @PathVariable Long channelId,
            @RequestBody ChannelRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return ResponseEntity.ok(channelService.updateChannel(channelId, request, userId));
    }

    // CH4 — Xóa channel
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        channelService.deleteChannel(id, userId);
        return ResponseEntity.noContent().build();
    }

    // Xóa tất cả channels của server (dùng nội bộ khi xóa server)
    @DeleteMapping("/server/{serverId}")
    public ResponseEntity<Void> deleteChannelsByServerId(@PathVariable Long serverId) {
        channelService.deleteChannelsByServerId(serverId);
        return ResponseEntity.noContent().build();
    }

    // CH6 — Ghim tin nhắn
    @PostMapping("/{channelId}/pins/{messageId}")
    public ResponseEntity<PinnedMessage> pinMessage(
            @PathVariable Long channelId,
            @PathVariable Long messageId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return ResponseEntity.ok(channelService.pinMessage(channelId, messageId, userId));
    }

    // CH6 — Bỏ ghim tin nhắn
    @DeleteMapping("/{channelId}/pins/{messageId}")
    public ResponseEntity<Map<String, String>> unpinMessage(
            @PathVariable Long channelId,
            @PathVariable Long messageId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        channelService.unpinMessage(channelId, messageId, userId);
        return ResponseEntity.ok(Map.of("message", "Đã bỏ ghim tin nhắn"));
    }

    // CH7 — Xem danh sách tin nhắn đã ghim
    @GetMapping("/{channelId}/pins")
    public ResponseEntity<List<PinnedMessage>> getPinnedMessages(@PathVariable Long channelId) {
        return ResponseEntity.ok(channelService.getPinnedMessages(channelId));
    }
}
