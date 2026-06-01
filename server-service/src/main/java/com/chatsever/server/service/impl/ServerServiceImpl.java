package com.chatsever.server.service.impl;

import com.chatsever.server.model.Member;
import com.chatsever.server.model.Server;
import com.chatsever.server.repository.MemberRepository;
import com.chatsever.server.repository.ServerRepository;
import com.chatsever.server.client.ChannelClient;
import com.chatsever.server.client.RoleClient;
import com.chatsever.server.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {
    private final ServerRepository serverRepository;
    private final MemberRepository memberRepository;
    private final ChannelClient channelClient;
    private final RoleClient roleClient;

    @Override
    @Transactional
    public Server createServer(Server server, String ownerId) {
        server.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        server.setOwnerId(ownerId);
        server.setIcon(toSafeRelativeMediaUrl(server.getIcon())); // defense-in-depth: chỉ lưu path nội bộ
        Server saved = serverRepository.save(server);

        // Khởi tạo danh sách roleIds rỗng, loại bỏ MemberRole.OWNER
        memberRepository.save(Member.builder()
                .serverId(saved.getId())
                .userId(ownerId)
                .roleIds(new ArrayList<>())
                .build());
                
        // Không khởi tạo 4 Role mặc định nữa theo yêu cầu của user (bỏ role null)
        // roleClient.initDefaultRoles(saved.getId());

        // Tạo kênh chat mặc định: "General"
        try {
            java.util.Map<String, Object> req = new java.util.HashMap<>();
            req.put("name", "General");
            req.put("serverId", saved.getId());
            req.put("type", "TEXT");
            channelClient.createChannel(req, ownerId);
        } catch (Exception e) {
            // Không hủy tiến trình nếu tạo kênh lỗi
        }
        
        return saved;
    }

    @Override
    public List<Server> getMyServers(String userId) {
        List<Long> ids = memberRepository.findByUserId(userId).stream().map(Member::getServerId).toList();
        return serverRepository.findAllById(ids);
    }

    // NF13 — Paginated version
    @Override
    public Page<Server> getMyServers(String userId, Pageable pageable) {
        List<Long> ids = memberRepository.findByUserId(userId).stream().map(Member::getServerId).toList();
        if (ids.isEmpty()) return Page.empty(pageable);
        return serverRepository.findByIdIn(ids, pageable);
    }

    @Override
    public Map<String, Object> getServerDetails(Long serverId) {
        Server s = serverRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("Server not found"));

        Map<String, Object> details = new HashMap<>();
        details.put("server", s);
        // Gọi API qua channel-service để lấy danh sách kênh (Microservices Inter-communication)
        details.put("channels", channelClient.getChannelsByServerId(serverId));
        details.put("members", memberRepository.findByServerId(serverId));

        return details;
    }

    @Override
    public Server updateServer(Long id, Server details, String uid) {
        Server s = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found"));

        if (!s.getOwnerId().equals(uid)) {
            checkPermission(id, uid, 8); // 8 = MANAGE_CHANNEL bitmask (or ADMIN)
        }

        s.setName(details.getName());
        s.setDescription(details.getDescription());
        s.setIcon(toSafeRelativeMediaUrl(details.getIcon())); // defense-in-depth: chỉ lưu path nội bộ
        return serverRepository.save(s);
    }

    /**
     * Chuẩn hóa & kiểm tra URL icon trước khi lưu DB (defense-in-depth).
     * - URL tuyệt đối / protocol-relative → chỉ giữ phần path (vô hiệu hóa host lạ).
     * - Chỉ chấp nhận path nội bộ "/api/files/...".
     * Trả về path tương đối an toàn, hoặc null nếu không hợp lệ.
     * Chống lộ Bearer token khi client tải icon từ host do attacker kiểm soát.
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
                return null;
            }
        }
        if (v.startsWith("/api/files/")) return v;
        return null;
    }

    @Override
    @Transactional
    public void deleteServer(Long id, String uid) {
        Server s = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found"));

        if (!s.getOwnerId().equals(uid)) {
            checkPermission(id, uid, 128); // 128 = ADMIN bitmask
        }

        memberRepository.deleteByServerId(id);
        // Gọi API qua channel-service để dọn dẹp các kênh liên quan
        channelClient.deleteChannelsByServerId(id);
        serverRepository.delete(s);
    }

    @Override
    public void joinServer(Long id, String code, String uid) {
        Server s = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found"));
        
        if(!code.equals(s.getInviteCode())) {
            throw new RuntimeException("Invalid invite code");
        }

        // R6 - Kiểm tra xem user có bị ban khỏi server này không
        try {
            Map<String, Object> banCheck = roleClient.checkBanned(id, uid);
            if (banCheck != null && Boolean.TRUE.equals(banCheck.get("banned"))) {
                throw new RuntimeException("Bạn đã bị cấm khỏi server này vĩnh viễn");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Log lỗi
        }

        if(!memberRepository.existsByServerIdAndUserId(s.getId(), uid)) {
            // Khởi tạo danh sách roleIds rỗng, loại bỏ MemberRole.MEMBER
            memberRepository.save(Member.builder()
                    .serverId(s.getId())
                    .userId(uid)
                    .roleIds(new ArrayList<>())
                    .build());
        }
    }

    @Override
    public Server joinServerByCode(String code, String uid) {
        Server s = serverRepository.findByInviteCode(code)
                .orElseThrow(() -> new RuntimeException("Invite code không hợp lệ"));
        
        // R6 - Kiểm tra xem user có bị ban khỏi server này không
        try {
            Map<String, Object> banCheck = roleClient.checkBanned(s.getId(), uid);
            if (banCheck != null && Boolean.TRUE.equals(banCheck.get("banned"))) {
                throw new RuntimeException("Bạn đã bị cấm khỏi server này vĩnh viễn");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Log lỗi
        }

        if(!memberRepository.existsByServerIdAndUserId(s.getId(), uid)) {
            memberRepository.save(Member.builder()
                    .serverId(s.getId())
                    .userId(uid)
                    .roleIds(new ArrayList<>())
                    .build());
        }
        return s;
    }

    @Override
    public void leaveServer(Long id, String uid) {
        Server s = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found"));

        // Kiểm tra quyền Owner bằng ownerId lưu trong Server thay vì Enum
        if(s.getOwnerId().equals(uid)) {
            throw new RuntimeException("Owner cannot leave the server");
        }

        Member m = memberRepository.findByServerIdAndUserId(id, uid)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        memberRepository.delete(m);
    }

    @Override
    public String generateNewInviteCode(Long id, String uid) {
        Server s = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found"));

        if(!s.getOwnerId().equals(uid)) throw new RuntimeException("No permission");

        s.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        return serverRepository.save(s).getInviteCode();
    }

    // R2 — Cập nhật roleIds cho member (gọi từ role-service)
    @Override
    @Transactional
    public void updateMemberRoles(Long serverId, String userId, List<Long> roleIds) {
        Member member = memberRepository.findByServerIdAndUserId(serverId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found: " + userId + " in server " + serverId));
        member.setRoleIds(roleIds);
        memberRepository.save(member);
    }

    // Auto-join: đảm bảo user là member của server (nếu chưa thì tự thêm)
    @Override
    @Transactional
    public void ensureMember(Long serverId, String userId) {
        serverRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("Server not found: " + serverId));
        if (!memberRepository.existsByServerIdAndUserId(serverId, userId)) {
            memberRepository.save(Member.builder()
                    .serverId(serverId)
                    .userId(userId)
                    .roleIds(new ArrayList<>())
                    .build());
        }
    }
    
    private void checkPermission(Long serverId, String userId, int requiredPermissionBit) {
        try {
            Map<String, Object> perms = roleClient.getPermissions(serverId, userId);
            if (perms != null && perms.containsKey("permissionBitmask")) {
                int bitmask = (int) perms.get("permissionBitmask");
                // Check nếu có quyền tương ứng, HOẶC có quyền ADMIN (128), HOẶC OWNER (255)
                if ((bitmask & requiredPermissionBit) != 0 || (bitmask & 128) != 0 || bitmask == 255) {
                    return; // Có quyền
                }
            }
            throw new RuntimeException("Bạn không có đủ quyền (cần " + requiredPermissionBit + " hoặc ADMIN)");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kiểm tra phân quyền: " + e.getMessage());
        }
    }
}