package com.chatsever.friend.controller;

import com.chatsever.friend.entity.Friendship;
import com.chatsever.friend.entity.FriendshipStatus;
import com.chatsever.friend.repository.FriendshipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendshipRepository repository;

    public FriendController(FriendshipRepository repository) {
        this.repository = repository;
    }

    // 1. Get accepted friends
    @GetMapping
    public ResponseEntity<List<String>> getFriends(@RequestHeader("X-User-Id") String userId) {
        List<Friendship> friendships = repository.findAllByUserAndStatus(userId, FriendshipStatus.ACCEPTED);
        List<String> friends = friendships.stream()
                .map(f -> f.getRequester().equals(userId) ? f.getAddressee() : f.getRequester())
                .collect(Collectors.toList());
        return ResponseEntity.ok(friends);
    }

    // 2. Get pending requests
    @GetMapping("/pending")
    public ResponseEntity<List<String>> getPendingRequests(@RequestHeader("X-User-Id") String userId) {
        List<Friendship> pending = repository.findPendingRequestsForUser(userId);
        List<String> requesters = pending.stream()
                .map(Friendship::getRequester)
                .collect(Collectors.toList());
        return ResponseEntity.ok(requesters);
    }

    // 3. Send friend request
    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestHeader("X-User-Id") String userId, @RequestBody Map<String, String> body) {
        String targetUser = body.get("targetUsername");
        if (targetUser == null || targetUser.equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username không hợp lệ"));
        }

        // Check if user exists via user-profile-service
        RestTemplate restTemplate = new RestTemplate();
        String userProfileUrl = System.getenv("USER_PROFILE_URL");
        if (userProfileUrl == null) userProfileUrl = "http://user-profile-service:8090";
        try {
            restTemplate.getForEntity(userProfileUrl + "/api/users/" + targetUser + "/profile", Object.class);
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy username này trong hệ thống."));
        } catch (Exception e) {
            // Log error, assume user exists or fallback
        }

        Optional<Friendship> existing = repository.findFriendship(userId, targetUser);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == FriendshipStatus.ACCEPTED) {
                return ResponseEntity.badRequest().body(Map.of("message", "Đã có bạn bè trong danh sách."));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Đã gửi lời mời hoặc đang chờ xác nhận."));
        }

        Friendship friendship = Friendship.builder()
                .requester(userId)
                .addressee(targetUser)
                .status(FriendshipStatus.PENDING)
                .build();
        repository.save(friendship);
        return ResponseEntity.ok(Map.of("message", "Đã gửi lời mời kết bạn thành công đến " + targetUser));
    }

    // 4. Accept friend request
    @PostMapping("/accept")
    public ResponseEntity<?> acceptRequest(@RequestHeader("X-User-Id") String userId, @RequestBody Map<String, String> body) {
        String requester = body.get("targetUsername");
        if (requester == null) return ResponseEntity.badRequest().body(Map.of("message", "Missing targetUsername"));

        Optional<Friendship> existing = repository.findFriendship(userId, requester);
        if (existing.isEmpty() || existing.get().getStatus() != FriendshipStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("message", "No pending request found"));
        }

        Friendship friendship = existing.get();
        if (!friendship.getAddressee().equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "You can only accept requests addressed to you"));
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        repository.save(friendship);
        return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
    }

    // 5. Reject or remove friend
    @PostMapping("/reject")
    public ResponseEntity<?> rejectOrRemove(@RequestHeader("X-User-Id") String userId, @RequestBody Map<String, String> body) {
        String targetUser = body.get("targetUsername");
        if (targetUser == null) return ResponseEntity.badRequest().body(Map.of("message", "Missing targetUsername"));

        Optional<Friendship> existing = repository.findFriendship(userId, targetUser);
        if (existing.isPresent()) {
            repository.delete(existing.get());
            return ResponseEntity.ok(Map.of("message", "Friendship or request removed"));
        }
        return ResponseEntity.ok(Map.of("message", "Nothing to remove"));
    }
}
