package network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class RoleApiClient {

    private final ObjectMapper json = JsonMapper.get();

    // R1 - Lấy danh sách roles của server
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRoles(long serverId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers/" + serverId + "/roles";
        HttpResponse<String> resp = sendGet(url);
        try {
            return json.readValue(resp.body(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new ApiException("Lỗi parse roles: " + e.getMessage(), e);
        }
    }

    // R1 - Tạo role mới
    public Map<String, Object> createRole(long serverId, String roleName, String color, String permissions) {
        Map<String, String> body = Map.of(
            "roleName", roleName,
            "color", color,
            "permissions", permissions
        );
        return postJson("/api/servers/" + serverId + "/roles", body);
    }

    // R1 - Xóa role
    public Map<String, String> deleteRole(String roleId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers/roles/" + roleId;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + SessionManager.get().getAccessToken())
                    .DELETE()
                    .build();
            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(), parseError(resp.body()));
            }
            return json.readValue(resp.body(), new TypeReference<Map<String, String>>() {});
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi gọi DELETE " + url + ": " + e.getMessage(), e);
        }
    }

    // R1 - Cập nhật role
    public Map<String, Object> updateRole(String roleId, String roleName, String color, String permissions) {
        Map<String, String> body = Map.of(
            "roleName", roleName,
            "color", color,
            "permissions", permissions
        );
        return putJson("/api/servers/roles/" + roleId, body);
    }

    // R2 - Gán role cho member
    public Map<String, Object> assignRoles(long serverId, String userId, List<String> roleIds) {
        Map<String, Object> body = Map.of("roleIds", roleIds);
        return putJson("/api/servers/" + serverId + "/members/" + userId + "/roles", body);
    }

    // R5 - Kick member
    public Map<String, String> kickMember(long serverId, String userId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers/" + serverId + "/kick/" + userId;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + SessionManager.get().getAccessToken())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(), parseError(resp.body()));
            }
            return json.readValue(resp.body(), new TypeReference<Map<String, String>>() {});
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi gọi " + url + ": " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách roleId (String) hiện tại của 1 member trong server.
     * Gọi GET /api/servers/{serverId} và extract members[].roleIds cho userId tương ứng.
     * Trả về List rỗng nếu không tìm thấy hoặc lỗi.
     */
    @SuppressWarnings("unchecked")
    public List<String> getMemberRoleIds(long serverId, String userId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers/" + serverId;
        try {
            HttpResponse<String> resp = sendGet(url);
            Map<String, Object> body = json.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> members = (List<Map<String, Object>>) body.get("members");
            if (members == null) return List.of();
            for (Map<String, Object> m : members) {
                if (userId.equals(String.valueOf(m.get("userId")))) {
                    Object roleIdsObj = m.get("roleIds");
                    if (roleIdsObj instanceof List<?> list) {
                        return list.stream()
                                .map(o -> String.valueOf(((Number) o).longValue()))
                                .toList();
                    }
                }
            }
        } catch (Exception ignored) {}
        return List.of();
    }


    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

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
