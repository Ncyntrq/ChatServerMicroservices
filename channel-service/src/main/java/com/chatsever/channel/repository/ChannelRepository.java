package com.chatsever.channel.repository;

import com.chatsever.channel.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByServerId(Long serverId);
    void deleteByServerId(Long serverId);
}
