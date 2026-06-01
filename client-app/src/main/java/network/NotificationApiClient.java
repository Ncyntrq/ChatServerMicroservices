package network;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class NotificationApiClient {

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUnreadCounts(String userId) throws ApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ApiConfig.GATEWAY_HTTP + "/api/notifications/unread-count?userId=" + userId))
                    .GET()
                    .header("Authorization", "Bearer " + SessionManager.get().getAccessToken())
                    .build();

            HttpResponse<String> response = HttpClientHolder.get()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return JsonMapper.get().readValue(response.body(), Map.class);
            } else {
                throw new ApiException(response.statusCode(), "Failed to get unread counts");
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException(500, e.getMessage());
        }
    }

    public void ackChannel(Long channelId, String userId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ApiConfig.GATEWAY_HTTP + "/api/notifications/ack-channel/" + channelId + "?userId=" + userId))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Authorization", "Bearer " + SessionManager.get().getAccessToken())
                    .build();
            HttpClientHolder.get().send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void ackDm(String senderUsername, String userId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ApiConfig.GATEWAY_HTTP + "/api/notifications/ack-dm/" + senderUsername + "?userId=" + userId))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Authorization", "Bearer " + SessionManager.get().getAccessToken())
                    .build();
            HttpClientHolder.get().send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
