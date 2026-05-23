package com.chatsever.messaging.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "server-service", url = "${services.server-url}")
public interface ServerServiceClient {
    @GetMapping("/api/servers/{id}")
    Map<String, Object> getServerDetails(@PathVariable("id") Long id);

    @PostMapping("/api/servers/{id}/ensure-member")
    Map<String, String> ensureMember(@PathVariable("id") Long id, @RequestParam("userId") String userId);
}
