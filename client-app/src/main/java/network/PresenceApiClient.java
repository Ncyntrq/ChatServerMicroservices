package network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST client cho presence-service (đi qua gateway: /api/presence/**).
 * Cung cấp lấy danh sách online, kiểm tra trạng thái user.
 */
public class PresenceApiClient {

    private final ObjectMapper json = JsonMapper.get();

    // P3 — Danh sách user đang online
    public List<String> getOnlineUsers() {
        String url = ApiConfig.GATEWAY_HTTP + "/api/presence/online";
        HttpResponse<String> resp = sendGet(url);
        try {
            return json.readValue(resp.body(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new ApiException("Lỗi parse danh sách online: " + e.getMessage(), e);
        }
    }

    // P4 — Kiểm tra trạng thái 1 user
    public Map<String, Object> getUserStatus(String userId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/presence/status/" + userId;
        HttpResponse<String> resp = sendGet(url);
        try {
            return json.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ApiException("Lỗi parse trạng thái user: " + e.getMessage(), e);
        }
    }

    // P5 — Cập nhật trạng thái hoạt động (ONLINE / IDLE / AWAY / DO_NOT_DISTURB / INVISIBLE)
    public void updatePresenceStatus(String status) {
        String token = SessionManager.get().getAccessToken();
        if (token == null) throw new ApiException("Chưa đăng nhập — không có token");
        String url = ApiConfig.GATEWAY_HTTP + "/api/presence/status?status=" + status;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(), parseError(resp.body()));
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Không cập nhật được trạng thái: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private HttpResponse<String> sendGet(String url) {
        String token = SessionManager.get().getAccessToken();
        if (token == null) throw new ApiException("Chưa đăng nhập — không có token");
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(), parseError(resp.body()));
            }
            return resp;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Không gọi được " + url + ": " + e.getMessage(), e);
        }
    }

    private String parseError(String body) {
        if (body == null || body.isBlank()) return "Lỗi từ server";
        try {
            Map<?, ?> m = json.readValue(body, Map.class);
            Object msg = m.get("message");
            if (msg != null) return msg.toString();
            Object err = m.get("error");
            if (err != null) return err.toString();
        } catch (Exception ignore) {}
        return body;
    }
}
