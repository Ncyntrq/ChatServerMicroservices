package com.chatsever.messaging.config;

import com.chatsever.messaging.handler.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Cấu hình WebSocket cho messaging-service.
 * Đăng ký endpoint /ws/chat để client kết nối real-time.
 */
@Configuration
@EnableWebSocket
@SuppressWarnings("null")
public class WebSocketConfig implements WebSocketConfigurer {

    // Handler xử lý tin nhắn WebSocket (nhận/gửi message)
    private final ChatWebSocketHandler chatHandler;

    public WebSocketConfig(ChatWebSocketHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Đăng ký handler tại đường dẫn /ws/chat, cho phép mọi origin (CORS)
        registry.addHandler(chatHandler, "/ws/chat").setAllowedOrigins("*");
    }
}