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
 * REST client cho server-service (đi qua gateway: /api/servers/**).
 * Cung cấp CRUD server, join/leave, invite code.
 */
public class ServerApiClient {

    private final ObjectMapper json = JsonMapper.get();

    // SV1 — Tạo server mới
    public Map<String, Object> createServer(String name, String description, String iconUrl) {
        Map<String, String> body = new java.util.HashMap<>();
        body.put("name", name);
        if (description != null) body.put("description", description);
        if (iconUrl != null) body.put("icon", iconUrl);
        return postJson("/api/servers", body);
    }

    // SV2 — Danh sách server đã tham gia
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMyServers() {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers";
        HttpResponse<String> resp = sendGet(url);
        try {
            // Backend có thể trả Page hoặc List tùy param
            Object parsed = json.readValue(resp.body(), Object.class);
            if (parsed instanceof List) {
                return (List<Map<String, Object>>) parsed;
            }
            // Nếu trả Page object, lấy field "content"
            if (parsed instanceof Map) {
                Map<?, ?> pageObj = (Map<?, ?>) parsed;
                Object content = pageObj.get("content");
                if (content instanceof List) {
                    return (List<Map<String, Object>>) content;
                }
            }
            return List.of();
        } catch (Exception e) {
            throw new ApiException("Lỗi parse danh sách server: " + e.getMessage(), e);
        }
    }

    // SV3 — Chi tiết server (bao gồm channels + members)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getServerDetails(long serverId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers/" + serverId;
        HttpResponse<String> resp = sendGet(url);
        try {
            return json.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ApiException("Lỗi parse chi tiết server: " + e.getMessage(), e);
        }
    }

    // SV4 — Cập nhật server (đổi tên, mô tả)
    public Map<String, Object> updateServer(long serverId, String name, String description, String iconUrl) {
        Map<String, String> body = new java.util.HashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (iconUrl != null) body.put("icon", iconUrl);
        return putJson("/api/servers/" + serverId, body);
    }

    // SV5 — Xóa server
    public void deleteServer(long serverId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers/" + serverId;
        sendDelete(url);
    }

    // SV6 — Tham gia server bằng invite code
    public void joinServer(long serverId, String inviteCode) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers/" + serverId + "/join?code=" + inviteCode;
        sendPost(url, "");
    }

    @SuppressWarnings("unchecked")
    public long joinServerByCode(String inviteCode) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers/join?code=" + inviteCode;
        HttpResponse<String> resp = sendPost(url, "");
        try {
            Map<String, Object> body = json.readValue(resp.body(), Map.class);
            if (body.get("serverId") != null) {
                return Long.parseLong(body.get("serverId").toString());
            }
        } catch (Exception ignore) {}
        return -1;
    }

    // SV7 — Rời server
    public void leaveServer(long serverId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers/" + serverId + "/leave";
        sendPost(url, "");
    }

    // SV8 — Tạo invite code
    @SuppressWarnings("unchecked")
    public String createInviteCode(long serverId) {
        Map<String, Object> resp = postJson("/api/servers/" + serverId + "/invite", Map.of());
        Object code = resp.get("inviteCode");
        return code != null ? code.toString() : "";
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJson(String path, Object body) {
        String url = ApiConfig.GATEWAY_HTTP + path;
        try {
            String payload = json.writeValueAsString(body);
            HttpResponse<String> resp = sendPost(url, payload);
            return json.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi gọi " + url + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
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

    private HttpResponse<String> sendPost(String url, String payload) {
        String token = SessionManager.get().getAccessToken();
        if (token == null) throw new ApiException("Chưa đăng nhập — không có token");
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token);
            if (payload != null && !payload.isEmpty()) {
                builder.header("Content-Type", "application/json")
                       .POST(HttpRequest.BodyPublishers.ofString(payload));
            } else {
                builder.POST(HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> resp = HttpClientHolder.get().send(builder.build(), HttpResponse.BodyHandlers.ofString());
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

    private void sendDelete(String url) {
        String token = SessionManager.get().getAccessToken();
        if (token == null) throw new ApiException("Chưa đăng nhập — không có token");
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .DELETE()
                    .build();
            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(), parseError(resp.body()));
            }
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
