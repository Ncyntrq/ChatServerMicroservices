package network;

import com.chatsever.common.dto.AuthRequest;
import com.chatsever.common.dto.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * REST client cho auth-service (đi qua gateway: /api/auth/**).
 * Sau login thành công, JWT được lưu vào SessionManager.
 */
public class AuthApiClient {

    private final ObjectMapper json = JsonMapper.get();

    public AuthResponse login(String username, String password) {
        AuthRequest body = new AuthRequest(username, password);
        AuthResponse resp = postJson("/api/auth/login", body, AuthResponse.class);
        SessionManager.get().setSession(resp.getToken(), resp.getRefreshToken(), resp.getUsername());
        return resp;
    }

    /** Trả về message text từ server (vd: "Đăng ký thành công"). */
    public String register(String username, String password) {
        AuthRequest body = new AuthRequest(username, password);
        HttpResponse<String> resp = sendJson("/api/auth/register", body);
        return resp.body();
    }

    public AuthResponse refresh(String refreshToken) {
        Map<String, String> body = Map.of("refreshToken", refreshToken);
        return postJson("/api/auth/refresh", body, AuthResponse.class);
    }

    /** A6 — Đổi mật khẩu. Gateway tự inject X-User-Id từ JWT. */
    public String changePassword(String oldPassword, String newPassword) {
        Map<String, String> body = Map.of("oldPassword", oldPassword, "newPassword", newPassword);
        String url = ApiConfig.GATEWAY_HTTP + "/api/auth/change-password";
        String token = SessionManager.get().getAccessToken();
        if (token == null) throw new ApiException("Chưa đăng nhập — không có token");
        try {
            String payload = json.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(), parseError(resp.body()));
            }
            try {
                Map<?, ?> m = json.readValue(resp.body(), Map.class);
                Object msg = m.get("message");
                return msg != null ? msg.toString() : "Đổi mật khẩu thành công";
            } catch (Exception ignore) {
                return "Đổi mật khẩu thành công";
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Không gọi được " + url + ": " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private <T> T postJson(String path, Object body, Class<T> responseType) {
        HttpResponse<String> resp = sendJson(path, body);
        try {
            return json.readValue(resp.body(), responseType);
        } catch (Exception e) {
            throw new ApiException("Lỗi parse JSON từ " + path + ": " + e.getMessage(), e);
        }
    }

    private HttpResponse<String> sendJson(String path, Object body) {
        String url = ApiConfig.GATEWAY_HTTP + path;
        try {
            String payload = json.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
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

    /** Trích message lỗi từ body JSON {"message": "..."} nếu có, fallback raw body. */
    private String parseError(String body) {
        if (body == null || body.isBlank()) return "Lỗi từ server";
        try {
            Map<?, ?> m = json.readValue(body, Map.class);
            Object msg = m.get("message");
            if (msg != null) return msg.toString();
            Object err = m.get("error");
            if (err != null) return err.toString();
        } catch (Exception ignore) {
            // không phải JSON, dùng raw
        }
        return body;
    }
}
