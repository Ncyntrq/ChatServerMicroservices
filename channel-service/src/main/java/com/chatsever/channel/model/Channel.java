package com.chatsever.channel.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "channels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Long serverId;

    @Enumerated(EnumType.STRING)
    private ChannelType type;
}
