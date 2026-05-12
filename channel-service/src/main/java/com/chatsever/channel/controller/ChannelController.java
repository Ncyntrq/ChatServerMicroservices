package com.chatsever.channel.controller;

import com.chatsever.channel.dto.ChannelRequest;
import com.chatsever.channel.service.ChannelService;
import com.chatsever.common.dto.ChannelDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    public ResponseEntity<ChannelDto> createChannel(@RequestBody ChannelRequest request) {
        return ResponseEntity.ok(channelService.createChannel(request));
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<ChannelDto>> getChannelsByServerId(@PathVariable Long serverId) {
        return ResponseEntity.ok(channelService.getChannelsByServerId(serverId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id) {
        channelService.deleteChannel(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/server/{serverId}")
    public ResponseEntity<Void> deleteChannelsByServerId(@PathVariable Long serverId) {
        channelService.deleteChannelsByServerId(serverId);
        return ResponseEntity.noContent().build();
    }
}
