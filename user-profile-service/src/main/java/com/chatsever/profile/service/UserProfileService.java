package com.chatsever.profile.service;

import com.chatsever.profile.model.UserProfile;
import com.chatsever.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service xử lý hồ sơ người dùng (UP1-UP5).
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png");

    private final UserProfileRepository repository;

    @Value("${avatar.upload-dir:./uploads/avatars}")
    private String uploadDir;

    // UP1 — Xem hồ sơ
    public UserProfile getProfile(String username) {
        return repository.findByUsername(username).orElseGet(() -> {
            UserProfile newProfile = UserProfile.builder()
                    .username(username)
                    .displayName(username)
                    .build();
            return repository.save(newProfile);
        });
    }

    // UP2 — Cập nhật hồ sơ
    public UserProfile updateProfile(String username, String displayName, String bio, String avatarUrl) {
        UserProfile profile = getProfile(username);
        if (displayName != null) profile.setDisplayName(displayName);
        if (bio != null) profile.setBio(bio);
        if (avatarUrl != null) {
            // Defense-in-depth: chỉ lưu path tương đối nội bộ, từ chối URL tuyệt đối tới host lạ
            String safe = toSafeRelativeMediaUrl(avatarUrl);
            if (safe != null) profile.setAvatarUrl(safe);
        }
        return repository.save(profile);
    }

    /**
     * Chuẩn hóa & kiểm tra URL ảnh trước khi lưu DB (defense-in-depth).
     * - Nếu là URL tuyệt đối / protocol-relative (chứa "://" hoặc bắt đầu "//"),
     *   chỉ giữ lại phần path → vô hiệu hóa host do attacker kiểm soát.
     * - Chỉ chấp nhận path nội bộ ("/api/files/..." hoặc "/api/users/avatars/...").
     * Trả về path tương đối an toàn, hoặc null nếu không hợp lệ (bỏ qua, không ghi đè).
     * Lý do: chống lộ Bearer token khi client tải ảnh từ host lạ (xem guard phía client).
     */
    private static String toSafeRelativeMediaUrl(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if (v.contains("://") || v.startsWith("//")) {
            try {
                String path = java.net.URI.create(v).getPath();
                v = path == null ? "" : path;
            } catch (Exception e) {
                return null; // URL dị dạng → từ chối
            }
        }
        if (v.startsWith("/api/files/") || v.startsWith("/api/users/avatars/")) return v;
        return null; // ngoài whitelist nội bộ → từ chối
    }

    // UP3 — Upload avatar (multipart, max 2MB, JPEG/PNG)
    public UserProfile uploadAvatar(String username, MultipartFile file) {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File avatar không được rỗng");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new RuntimeException("File avatar vượt quá kích thước cho phép (2MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new RuntimeException("Chỉ chấp nhận file JPEG hoặc PNG");
        }

        try {
            // Tạo thư mục upload nếu chưa có
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            // Tạo tên file unique
            String extension = contentType.equals("image/png") ? ".png" : ".jpg";
            String storedName = UUID.randomUUID() + extension;

            // Lưu file
            Path filePath = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Cập nhật avatar URL trong profile
            String avatarUrl = "/api/users/avatars/" + storedName;
            UserProfile profile = getProfile(username);
            profile.setAvatarUrl(avatarUrl);
            UserProfile saved = repository.save(profile);

            log.info("Avatar uploaded: user={}, file={}, size={}",
                    username, storedName, file.getSize());
            return saved;

        } catch (IOException e) {
            log.error("Lỗi upload avatar: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể upload avatar: " + e.getMessage());
        }
    }

    // UP4 — Đặt trạng thái tùy chỉnh
    public UserProfile updateStatus(String username, String customStatus) {
        UserProfile profile = getProfile(username);
        profile.setCustomStatus(customStatus);
        return repository.save(profile);
    }

    // UP5 — Tìm kiếm user
    public List<UserProfile> searchUsers(String keyword) {
        return repository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(keyword, keyword);
    }
}