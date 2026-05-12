package com.chatsever.server.service.impl;

import com.chatsever.server.model.*;
import com.chatsever.server.repository.*;
import com.chatsever.server.client.ChannelClient;
import com.chatsever.server.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
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

        memberRepository.save(Member.builder()
                .serverId(saved.getId()).userId(ownerId).role(MemberRole.OWNER).build());
        return saved;
    }

    @Override
    public List<Server> getMyServers(String userId) {
        List<Long> ids = memberRepository.findByUserId(userId).stream().map(Member::getServerId).toList();
        return serverRepository.findAllById(ids);
    }

    @Override
    public Map<String, Object> getServerDetails(Long serverId) {
        Server s = serverRepository.findById(serverId).orElseThrow();
        Map<String, Object> details = new HashMap<>();
        details.put("server", s);
        details.put("channels", channelClient.getChannelsByServerId(serverId));
        details.put("members", memberRepository.findByServerId(serverId));
        return details;
    }

    @Override
    public Server updateServer(Long id, Server details, String uid) {
        Server s = serverRepository.findById(id).orElseThrow();
        if(!s.getOwnerId().equals(uid)) throw new RuntimeException("No permission");
        s.setName(details.getName());
        s.setDescription(details.getDescription());
        return serverRepository.save(s);
    }

    @Override
    @Transactional
    public void deleteServer(Long id, String uid) {
        Server s = serverRepository.findById(id).orElseThrow();
        if(!s.getOwnerId().equals(uid)) throw new RuntimeException("No permission");
        memberRepository.deleteByServerId(id);
        channelClient.deleteChannelsByServerId(id);
        serverRepository.delete(s);
    }

    @Override
    public void joinServer(String code, String uid) {
        Server s = serverRepository.findByInviteCode(code).orElseThrow();
        if(!memberRepository.existsByServerIdAndUserId(s.getId(), uid)) {
            memberRepository.save(Member.builder().serverId(s.getId()).userId(uid).role(MemberRole.MEMBER).build());
        }
    }

    @Override
    public void leaveServer(Long id, String uid) {
        Member m = memberRepository.findByServerIdAndUserId(id, uid).orElseThrow();
        if(m.getRole() == MemberRole.OWNER) throw new RuntimeException("Owner cannot leave");
        memberRepository.delete(m);
    }

    @Override
    public String generateNewInviteCode(Long id, String uid) {
        Server s = serverRepository.findById(id).orElseThrow();
        if(!s.getOwnerId().equals(uid)) throw new RuntimeException("No permission");
        s.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        return serverRepository.save(s).getInviteCode();
    }
}