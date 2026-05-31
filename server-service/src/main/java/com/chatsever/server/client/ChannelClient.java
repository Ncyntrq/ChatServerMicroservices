package com.chatsever.server.client;

import com.chatsever.common.dto.ChannelDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "channel-service", url = "${CHANNEL_SERVICE_URL:http://localhost:8086}")
public interface ChannelClient {
    
    @GetMapping("/api/channels/server/{serverId}")
    List<ChannelDto> getChannelsByServerId(@PathVariable("serverId") Long serverId);

    @org.springframework.web.bind.annotation.PostMapping("/api/channels")
    ChannelDto createChannel(
        @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> request,
        @org.springframework.web.bind.annotation.RequestHeader("X-User-Id") String userId
    );

    @DeleteMapping("/api/channels/server/{serverId}")
    void deleteChannelsByServerId(@PathVariable("serverId") Long serverId);
}
