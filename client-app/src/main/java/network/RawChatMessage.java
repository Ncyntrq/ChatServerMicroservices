package network;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mirror entity ChatMessage trả về từ messaging-service.
 * Field `id` map sang `messageId` của MessageDTO.
 * Chỉ dùng cho deserialize history; không lưu logic.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawChatMessage {
    public Long id;
    public String sender;
    public String content;
    public String receiver;
    public Long channelId;
    public Long serverId;
    public LocalDateTime timestamp;
    public MessageType type;
    public Boolean isEdited;
    public Boolean isDeleted;
    public Long replyToMessageId;
    public String replyToSender;
    public String replyToContent;
    public List<RawReaction> reactions;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawReaction {
        public Long id;
        public Long messageId;
        public String userId;
        public String emoji;
        public int count;
    }

    public MessageDTO toDto() {
        MessageDTO dto = new MessageDTO(
                type != null ? type : MessageType.CHAT,
                sender,
                receiver,
                content,
                timestamp
        );
        dto.setChannelId(channelId);
        dto.setServerId(serverId);
        dto.setMessageId(id);
        dto.setIsEdited(isEdited);
        dto.setIsDeleted(isDeleted);
        dto.setReplyToMessageId(replyToMessageId);
        dto.setReplyToSender(replyToSender);
        dto.setReplyToContent(replyToContent);

        if (reactions != null && !reactions.isEmpty()) {
            List<MessageDTO.ReactionDTO> mapped = reactions.stream()
                    .map(r -> new MessageDTO.ReactionDTO(r.userId, r.emoji, r.count))
                    .toList();
            dto.setReactions(mapped);
        }

        return dto;
    }
}
