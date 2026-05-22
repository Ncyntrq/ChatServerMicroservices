package com.chatsever.server.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "role-service", url = "${ROLE_SERVICE_URL:http://localhost:8091}")
public interface RoleClient {

    @PostMapping("/api/servers/{serverId}/roles/init")
    void initDefaultRoles(@PathVariable("serverId") Long serverId);

    @GetMapping("/api/servers/{serverId}/ban/{userId}/check")
    Map<String, Object> checkBanned(@PathVariable("serverId") Long serverId, @PathVariable("userId") String userId);
    
    @GetMapping("/api/servers/{serverId}/permissions/{userId}")
    Map<String, Object> getPermissions(@PathVariable("serverId") Long serverId, @PathVariable("userId") String userId);
}
