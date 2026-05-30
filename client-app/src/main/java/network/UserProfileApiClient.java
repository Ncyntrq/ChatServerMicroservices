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
 * REST client cho user-profile-service (đi qua gateway: /api/users/**).
 * Cung cấp xem/sửa profile, upload avatar, đặt trạng thái, tìm kiếm user.
 */
public class UserProfileApiClient {

    private final ObjectMapper json = JsonMapper.get();

    // UP1 — Xem hồ sơ user
    public Map<String, Object> getProfile(String username) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/users/" + username + "/profile";
        HttpResponse<String> resp = sendGet(url);
        try {
            return json.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ApiException("Lỗi parse profile: " + e.getMessage(), e);
        }
    }

    // UP2 — Cập nhật hồ sơ (displayName, bio)
    public Map<String, Object> updateProfile(String displayName, String bio) {
        Map<String, String> body = new java.util.HashMap<>();
        if (displayName != null) body.put("displayName", displayName);
        if (bio != null) body.put("bio", bio);
        return putJson("/api/users/profile", body);
    }

    // UP4 — Đặt trạng thái tùy chỉnh
    public Map<String, Object> updateStatus(String status) {
        Map<String, String> body = Map.of("status", status);
        return putJson("/api/users/status", body);
    }

    // UP5 — Tìm kiếm user
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchUsers(String keyword) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/users/search?q=" + keyword;
        HttpResponse<String> resp = sendGet(url);
        try {
            return json.readValue(resp.body(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new ApiException("Lỗi parse kết quả tìm kiếm: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Map<String, Object> putJson(String path, Object body) {
        String url = ApiConfig.GATEWAY_HTTP + path;
        try {
            String payload = json.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + SessionManager.get().getAccessToken())
                    .PUT(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(), parseError(resp.body()));
            }
            return json.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi gọi " + url + ": " + e.getMessage(), e);
        }
    }

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
