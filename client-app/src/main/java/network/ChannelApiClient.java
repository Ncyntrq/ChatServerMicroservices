package network;

import com.chatsever.common.dto.MessageDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST client cho channel-service (đi qua gateway).
 * - Lấy lịch sử tin nhắn: GET /api/channels/{channelId}/messages
 * - CRUD channel: GET /api/channels/server/{serverId}, POST /api/channels, PUT /api/channels/{id}, DELETE /api/channels/{id}
 *
 * Gateway dùng JWT từ Authorization header → inject X-User-Id.
 */
public class ChannelApiClient {

    private final ObjectMapper json = JsonMapper.get();

    /** Lấy `limit` tin nhắn gần nhất (theo id DESC). Trả về list đã đảo ngược (cũ → mới) để render trực tiếp. */
    public List<MessageDTO> fetchRecentMessages(long channelId, int limit) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/channels/" + channelId + "/messages?limit=" + limit;
        String token = SessionManager.get().getAccessToken();
        if (token == null) {
            throw new ApiException("Chưa đăng nhập — không có token");
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(),
                        "Không lấy được lịch sử kênh " + channelId + ": HTTP " + resp.statusCode());
            }

            // BE trả ChatMessage entity; cấu trúc tương thích với MessageDTO (sender, content, channelId, serverId, timestamp, type, isEdited, id→messageId)
            List<RawChatMessage> raw = json.readValue(resp.body(), new TypeReference<List<RawChatMessage>>() {});
            // Đảo ngược: BE order DESC theo id → ta render cũ trước (ASC)
            Collections.reverse(raw);
            return raw.stream().map(RawChatMessage::toDto).toList();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi gọi " + url + ": " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Channel CRUD
    // ---------------------------------------------------------------

    /** CH2 — Lấy danh sách channels của 1 server */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getChannelsByServer(long serverId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/channels/server/" + serverId;
        HttpResponse<String> resp = sendGet(url);
        try {
            return json.readValue(resp.body(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new ApiException("Lỗi parse danh sách channel: " + e.getMessage(), e);
        }
    }

    /** CH1 — Tạo channel mới */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createChannel(long serverId, String name, String type) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("serverId", serverId);
        body.put("name", name);
        body.put("type", type != null ? type : "TEXT");
        return postJson("/api/channels", body);
    }

    /** CH3 — Cập nhật channel (đổi tên, topic) */
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateChannel(long channelId, String name, String topic) {
        Map<String, String> body = new java.util.HashMap<>();
        if (name != null) body.put("name", name);
        if (topic != null) body.put("topic", topic);
        return putJson("/api/channels/" + channelId, body);
    }

    /** CH4 — Xóa channel */
    public void deleteChannel(long channelId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/channels/" + channelId;
        sendDelete(url);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJson(String path, Object body) {
        String url = ApiConfig.GATEWAY_HTTP + path;
        try {
            String payload = json.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + SessionManager.get().getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
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

    // ---------------------------------------------------------------
    // Pinned Messages
    // ---------------------------------------------------------------

    /** Ghim tin nhắn vào channel. */
    public void pinMessage(long channelId, long messageId) {
        postJson("/api/channels/" + channelId + "/pins/" + messageId, Map.of());
    }

    /** Bỏ ghim tin nhắn. */
    public void unpinMessage(long channelId, long messageId) {
        sendDelete(ApiConfig.GATEWAY_HTTP + "/api/channels/" + channelId + "/pins/" + messageId);
    }

    /** Lấy danh sách tin nhắn đã ghim (chỉ chứa messageId). */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPinnedMessages(long channelId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/channels/" + channelId + "/pins";
        HttpResponse<String> resp = sendGet(url);
        try {
            return json.readValue(resp.body(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new ApiException("Lỗi parse danh sách pin: " + e.getMessage(), e);
        }
    }

    /** Lấy nội dung tin nhắn theo danh sách IDs (dùng cho ghim). */
    public List<MessageDTO> getMessagesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        String idParams = ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        String url = ApiConfig.GATEWAY_HTTP + "/api/channels/bulk?ids=" + idParams;
        HttpResponse<String> resp = sendGet(url);
        try {
            List<RawChatMessage> raw = json.readValue(resp.body(), new TypeReference<List<RawChatMessage>>() {});
            return raw.stream().map(RawChatMessage::toDto).toList();
        } catch (Exception e) {
            throw new ApiException("Lỗi parse danh sách messages: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Tìm kiếm tin nhắn
    // ---------------------------------------------------------------

    /** Tìm kiếm tin nhắn theo từ khóa trong channel hoặc server. */
    public List<MessageDTO> searchMessages(Long channelId, Long serverId, String keyword, int limit) {
        StringBuilder url = new StringBuilder(ApiConfig.GATEWAY_HTTP + "/api/channels/search?keyword=");
        try {
            url.append(java.net.URLEncoder.encode(keyword, "UTF-8"));
        } catch (Exception e) {
            url.append(keyword);
        }
        url.append("&limit=").append(limit);
        if (channelId != null) url.append("&channelId=").append(channelId);
        if (serverId != null) url.append("&serverId=").append(serverId);
        HttpResponse<String> resp = sendGet(url.toString());
        try {
            List<RawChatMessage> raw = json.readValue(resp.body(), new TypeReference<List<RawChatMessage>>() {});
            return raw.stream().map(RawChatMessage::toDto).toList();
        } catch (Exception e) {
            throw new ApiException("Lỗi parse kết quả tìm kiếm: " + e.getMessage(), e);
        }
    }
}
