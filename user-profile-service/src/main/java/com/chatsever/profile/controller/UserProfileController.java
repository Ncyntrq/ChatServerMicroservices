package com.chatsever.profile.controller;

import com.chatsever.profile.model.UserProfile;
import com.chatsever.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    // UP1 — Xem hồ sơ
    @GetMapping("/{username}/profile")
    public ResponseEntity<UserProfile> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(profileService.getProfile(username));
    }

    // UP2 — Cập nhật hồ sơ (displayName, bio)
    @PutMapping("/profile")
    public ResponseEntity<UserProfile> updateProfile(@RequestHeader("X-User-Id") String username,
                                                     @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(profileService.updateProfile(username, payload.get("displayName"), payload.get("bio")));
    }

    // UP3 — Upload avatar (multipart, max 2MB, JPEG/PNG)
    @PostMapping("/avatar")
    public ResponseEntity<UserProfile> uploadAvatar(@RequestHeader("X-User-Id") String username,
                                                    @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(profileService.uploadAvatar(username, file));
    }

    // UP4 — Đặt trạng thái tùy chỉnh
    @PutMapping("/status")
    public ResponseEntity<UserProfile> updateStatus(@RequestHeader("X-User-Id") String username,
                                                    @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(profileService.updateStatus(username, payload.get("status")));
    }

    // UP5 — Tìm kiếm user
    @GetMapping("/search")
    public ResponseEntity<List<UserProfile>> searchUsers(@RequestParam("q") String keyword) {
        return ResponseEntity.ok(profileService.searchUsers(keyword));
    }
}