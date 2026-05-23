package network;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

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
    public Long channelId;
    public Long serverId;
    public LocalDateTime timestamp;
    public MessageType type;
    public Boolean isEdited;

    public MessageDTO toDto() {
        MessageDTO dto = new MessageDTO(
                type != null ? type : MessageType.CHAT,
                sender,
                null,
                content,
                timestamp
        );
        dto.setChannelId(channelId);
        dto.setServerId(serverId);
        dto.setMessageId(id);
        dto.setIsEdited(isEdited);
        return dto;
    }
}
