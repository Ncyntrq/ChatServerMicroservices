package network;

import com.chatsever.common.dto.MessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * WebSocket client kết nối tới messaging-service qua gateway:
 *   ws://localhost:8080/ws/chat?token=JWT
 *
 * Gửi/nhận MessageDTO JSON. Cài callback bằng setOnMessage(...) để render UI.
 */
public class ChatWebSocketClient {

    private final ObjectMapper json = JsonMapper.get();
    private final StringBuilder pendingText = new StringBuilder();

    private WebSocket socket;
    private Consumer<MessageDTO> onMessage = m -> {};
    private Consumer<String> onError = err -> System.err.println("[WS] " + err);
    private Runnable onClose = () -> {};

    public void setOnMessage(Consumer<MessageDTO> cb) { this.onMessage = cb; }
    public void setOnError(Consumer<String> cb) { this.onError = cb; }
    public void setOnClose(Runnable cb) { this.onClose = cb; }

    public CompletableFuture<Void> connect(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return CompletableFuture.failedFuture(new ApiException("Thiếu JWT để kết nối WebSocket"));
        }
        URI uri = URI.create(ApiConfig.GATEWAY_WS + ApiConfig.WS_CHAT_PATH + "?token=" + accessToken);
        return HttpClientHolder.get()
                .newWebSocketBuilder()
                .buildAsync(uri, new Listener())
                .thenAccept(ws -> this.socket = ws);
    }

    public boolean isOpen() {
        return socket != null && !socket.isOutputClosed();
    }

    public CompletableFuture<WebSocket> send(MessageDTO message) {
        if (socket == null) {
            return CompletableFuture.failedFuture(new ApiException("WebSocket chưa kết nối"));
        }
        try {
            String payload = json.writeValueAsString(message);
            return socket.sendText(payload, true);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                    new ApiException("Lỗi serialize MessageDTO: " + e.getMessage(), e));
        }
    }

    public void close() {
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "client closing");
        }
    }

    // ---------------------------------------------------------------
    // Listener: gom các fragment text rồi parse JSON → MessageDTO
    // ---------------------------------------------------------------
    private class Listener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            pendingText.append(data);
            if (last) {
                String payload = pendingText.toString();
                pendingText.setLength(0);
                try {
                    MessageDTO msg = json.readValue(payload, MessageDTO.class);
                    onMessage.accept(msg);
                } catch (Exception e) {
                    onError.accept("Lỗi parse message: " + e.getMessage() + " | payload=" + payload);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            onError.accept("WebSocket đóng: " + statusCode + " " + reason);
            onClose.run();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            onError.accept("WebSocket error: " + error.getMessage());
        }
    }
}
