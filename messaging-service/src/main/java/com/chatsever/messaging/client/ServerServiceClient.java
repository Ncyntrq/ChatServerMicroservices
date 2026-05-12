package com.chatsever.messaging.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "server-service", url = "${services.server-url}")
public interface ServerServiceClient {
    @GetMapping("/api/servers/{id}")
    Map<String, Object> getServerDetails(@PathVariable("id") Long id);
}
