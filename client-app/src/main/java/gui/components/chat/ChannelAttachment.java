package gui.components.chat;

import java.time.LocalDateTime;

/**
 * Mô tả 1 file/ảnh đính kèm trong channel hoặc DM — model dùng chung cho sidebar
 * Ảnh/Video & File. Nguồn: API channel (backend) hoặc trích từ tin nhắn đã tải (DM).
 * {@code url}/{@code thumbnailUrl} là URL ĐẦY ĐỦ qua gateway (sẵn sàng tải).
 */
public class ChannelAttachment {

    public final String contentType;
    public final String name;
    public final String url;
    public final String thumbnailUrl;
    public final long size;
    public final LocalDateTime createdAt; // có thể null nếu không rõ

    public ChannelAttachment(String contentType, String name, String url,
                             String thumbnailUrl, long size, LocalDateTime createdAt) {
        this.contentType = contentType;
        this.name = name;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
        this.size = size;
        this.createdAt = createdAt;
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }
}
