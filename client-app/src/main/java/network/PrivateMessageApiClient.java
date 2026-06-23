package network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatsever.common.dto.MessageDTO;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class PrivateMessageApiClient {
    private final ObjectMapper json = JsonMapper.get();

    public List<MessageDTO> fetchPrivateMessages(String targetUser, int limit) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/messages/private?targetUser=" + targetUser + "&limit=" + limit;
        String token = SessionManager.get().getAccessToken();
        if (token == null) throw new ApiException("Chưa đăng nhập");

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() / 100 != 2) {
                throw new ApiException("Lỗi lấy private messages: " + resp.body());
            }

            List<RawChatMessage> raw = json.readValue(resp.body(), new TypeReference<List<RawChatMessage>>() {});
            Collections.reverse(raw);
            return raw.stream().map(RawChatMessage::toDto).toList();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi gọi " + url + ": " + e.getMessage(), e);
        }
    }
}
