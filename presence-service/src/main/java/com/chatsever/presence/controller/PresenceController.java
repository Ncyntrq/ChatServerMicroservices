package com.chatsever.presence.controller;

import com.chatsever.presence.model.UserStatus;
import com.chatsever.presence.service.PresenceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /** API Test - Giữ lại để kiểm tra thông tuyến Gateway */
    @GetMapping("/test")
    public String test() {
        return "Presence Service is ONLINE - Chào mừng bạn đến với phòng 8083!";
    }

    /** 1. POST /connect - Gọi khi User mở app / đăng nhập */
    @PostMapping("/connect")
    public void connect(@RequestParam String username) {
        presenceService.connect(username);
    }

    /** 2. POST /disconnect - Gọi khi User tắt app / đăng xuất */
    @PostMapping("/disconnect")
    public void disconnect(@RequestParam String username) {
        presenceService.disconnect(username);
    }

    /** 3. GET /online - Lấy danh sách tất cả những người đang Online */
    @GetMapping("/online")
    public List<String> getOnlineUsers() {
        return presenceService.getOnlineUsers(); 
    }

    /** 4. GET /status/{username} - Xem trạng thái của một người cụ thể */
    @GetMapping("/status/{username}")
    public UserStatus getStatus(@PathVariable String username) {
        return presenceService.getUserStatus(username);
    }
}