package com.chatsever.server.service.impl;

import com.chatsever.server.model.Member;
import com.chatsever.server.model.Server;
import com.chatsever.server.repository.MemberRepository;
import com.chatsever.server.repository.ServerRepository;
import com.chatsever.server.client.ChannelClient;
import com.chatsever.server.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ServerServiceImpl implements ServerService {
    private final ServerRepository serverRepository;
    private final MemberRepository memberRepository;
    private final ChannelClient channelClient;

    @Override
    @Transactional
    public Server createServer(Server server, String ownerId) {
        server.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        server.setOwnerId(ownerId);
        Server saved = serverRepository.save(server);

        // Khởi tạo danh sách roleIds rỗng, loại bỏ MemberRole.OWNER
        memberRepository.save(Member.builder()
                .serverId(saved.getId())
                .userId(ownerId)
                .roleIds(new ArrayList<>())
                .build());
        return saved;
    }

    @Override
    public List<Server> getMyServers(String userId) {
        List<Long> ids = memberRepository.findByUserId(userId).stream().map(Member::getServerId).toList();
        return serverRepository.findAllById(ids);
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

        if(!s.getOwnerId().equals(uid)) throw new RuntimeException("No permission");

        s.setName(details.getName());
        s.setDescription(details.getDescription());
        s.setIcon(details.getIcon());
        return serverRepository.save(s);
    }

    @Override
    @Transactional
    public void deleteServer(Long id, String uid) {
        Server s = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found"));

        if(!s.getOwnerId().equals(uid)) throw new RuntimeException("No permission");

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
}