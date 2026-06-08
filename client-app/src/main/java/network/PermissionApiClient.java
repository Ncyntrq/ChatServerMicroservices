package network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Lấy quyền hiệu lực (effective permissions) của 1 user trong 1 server.
 * Backend: GET /api/servers/{serverId}/permissions/{userId} → trả "permissionBitmask".
 */
public class PermissionApiClient {

    private final ObjectMapper json = JsonMapper.get();

    /** Trả về permission bitmask; 0 nếu không lấy được (an toàn: coi như không có quyền). */
    public int getPermissionBitmask(long serverId, String userId) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/servers/" + serverId + "/permissions/" + userId;
        String token = SessionManager.get().getAccessToken();
        if (token == null) return 0;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) return 0;
            Map<String, Object> body = json.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
            Object bm = body.get("permissionBitmask");
            return (bm instanceof Number) ? ((Number) bm).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
