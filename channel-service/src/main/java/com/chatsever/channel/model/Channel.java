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

    // CH3: topic, slowmode
    private String topic;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer slowmode; // seconds between messages (0 = off)

    // CH5: category grouping
    private String category;

    // CH8: Pin channel
    private Long pinnedAt;
}
