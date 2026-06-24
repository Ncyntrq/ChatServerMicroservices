package com.chatsever.messaging.features.chat;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.domain.model.ChatMessage;
import com.chatsever.messaging.domain.vo.MessageContent;
import com.chatsever.messaging.domain.vo.UserId;
import com.chatsever.messaging.infrastructure.event.MessageEventPublisher;
import com.chatsever.messaging.infrastructure.persistence.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatCommandHandler {

    private final MessageRepository messageRepository;
    private final MessageEventPublisher eventPublisher;

    public ChatCommandHandler(MessageRepository messageRepository, MessageEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processChatMessage(MessageDTO msg) {
        ChatMessage saved = saveChannelMessage(msg);
        msg.setMessageId(saved.getId());
        eventPublisher.broadcastToChannel(msg);
        eventPublisher.publishNotificationEvent(msg);
        eventPublisher.publishLogEvent(msg);
    }

    @Transactional
    public void processPrivateMessage(MessageDTO msg) {
        ChatMessage saved = savePrivateMessage(msg);
        msg.setMessageId(saved.getId());
        eventPublisher.publishNotificationEvent(msg);
        eventPublisher.publishLogEvent(msg);
    }

    @Transactional
    public void processEditMessage(MessageDTO msg) {
        ChatMessage entity = messageRepository.findById(msg.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        entity.edit(new MessageContent(msg.getContent()));
        messageRepository.save(entity);
        
        msg.setIsEdited(true);
        eventPublisher.broadcastToChannel(msg);
        eventPublisher.publishLogEvent(msg);
    }

    @Transactional
    public void processDeleteMessage(MessageDTO msg) {
        ChatMessage entity = messageRepository.findById(msg.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        entity.delete();
        messageRepository.save(entity);
        
        // Handle unpinning
        eventPublisher.unpinDeletedMessage(entity.getChannelId(), msg.getMessageId());
        
        msg.setContent("");
        msg.setIsDeleted(true);
        eventPublisher.broadcastToChannel(msg);
        eventPublisher.publishLogEvent(msg);
    }

    private ChatMessage saveChannelMessage(MessageDTO msg) {
        ChatMessage entity = ChatMessage.createChannelMessage(
                new UserId(msg.getSender()),
                new MessageContent(msg.getContent()),
                msg.getChannelId(),
                msg.getServerId(),
                msg.getType(),
                msg.getReplyToMessageId()
        );
        return saveWithReplyInfo(entity, msg);
    }

    private ChatMessage savePrivateMessage(MessageDTO msg) {
        ChatMessage entity = ChatMessage.createPrivateMessage(
                new UserId(msg.getSender()),
                new UserId(msg.getReceiver()),
                new MessageContent(msg.getContent()),
                msg.getType(),
                msg.getReplyToMessageId()
        );
        return saveWithReplyInfo(entity, msg);
    }

    private ChatMessage saveWithReplyInfo(ChatMessage entity, MessageDTO msg) {
        ChatMessage saved = messageRepository.save(entity);
        if (msg.getReplyToMessageId() != null) {
            messageRepository.findById(msg.getReplyToMessageId()).ifPresent(original -> {
                msg.setReplyToSender(original.getSender());
                msg.setReplyToContent(original.getContent());
            });
        }
        return saved;
    }
}
