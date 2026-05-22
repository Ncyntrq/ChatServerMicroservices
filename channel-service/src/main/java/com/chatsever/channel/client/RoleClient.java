package com.chatsever.channel.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "role-service", url = "${ROLE_SERVICE_URL:http://localhost:8091}")
public interface RoleClient {

    @GetMapping("/api/servers/{serverId}/permissions/{userId}")
    Map<String, Object> getPermissions(@PathVariable("serverId") Long serverId, @PathVariable("userId") String userId);
}
