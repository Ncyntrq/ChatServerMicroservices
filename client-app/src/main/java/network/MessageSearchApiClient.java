package network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatsever.common.dto.MessageDTO;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * REST client cho tìm kiếm tin nhắn (messaging-service qua gateway: /api/messages/search).
 * Tái dùng RawChatMessage để deserialize giống lịch sử chat.
 */
public class MessageSearchApiClient {

    private final ObjectMapper json = JsonMapper.get();

    /** Tìm trong 1 channel. */
    public List<MessageDTO> searchInChannel(long channelId, String keyword, int limit) {
        return search("channel", keyword, "&channelId=" + channelId, limit);
    }

    /** Tìm trong cuộc trò chuyện DM với targetUser. */
    public List<MessageDTO> searchInPrivate(String targetUser, String keyword, int limit) {
        return search("private", keyword, "&targetUser=" + enc(targetUser), limit);
    }

    /** Tìm toàn cục theo user hiện tại. */
    public List<MessageDTO> searchAll(String keyword, int limit) {
        return search("all", keyword, "", limit);
    }

    private List<MessageDTO> search(String scope, String keyword, String extra, int limit) {
        String token = SessionManager.get().getAccessToken();
        if (token == null) throw new ApiException("Chưa đăng nhập");

        String url = ApiConfig.GATEWAY_HTTP + "/api/messages/search?q=" + enc(keyword)
                + "&scope=" + scope + "&limit=" + limit + extra;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException("Lỗi tìm kiếm tin nhắn: " + resp.body());
            }

            List<RawChatMessage> raw = json.readValue(resp.body(), new TypeReference<List<RawChatMessage>>() {});
            return raw.stream().map(RawChatMessage::toDto).toList();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi gọi " + url + ": " + e.getMessage(), e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
