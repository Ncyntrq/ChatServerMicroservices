package network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileApiClient {

    private final ObjectMapper json = JsonMapper.get();

    @SuppressWarnings("unchecked")
    public String uploadAvatar(File file) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/files/upload";
        String token = SessionManager.get().getAccessToken();
        String userId = SessionManager.get().getUsername();
        if (token == null || userId == null) throw new ApiException("Chưa đăng nhập");

        try {
            String boundary = "Boundary-" + UUID.randomUUID().toString();
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String fileName = file.getName();
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) mimeType = "application/octet-stream";

            List<byte[]> byteArrays = new ArrayList<>();
            String separator = "--" + boundary + "\r\n";
            
            // userId field
            StringBuilder sb = new StringBuilder();
            sb.append(separator);
            sb.append("Content-Disposition: form-data; name=\"userId\"\r\n\r\n");
            sb.append(userId).append("\r\n");
            
            // file field
            sb.append(separator);
            sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
            sb.append("Content-Type: ").append(mimeType).append("\r\n\r\n");
            
            byteArrays.add(sb.toString().getBytes(StandardCharsets.UTF_8));
            byteArrays.add(fileBytes);
            byteArrays.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            // Combine bytes
            int totalLen = 0;
            for (byte[] b : byteArrays) totalLen += b.length;
            byte[] body = new byte[totalLen];
            int destPos = 0;
            for (byte[] b : byteArrays) {
                System.arraycopy(b, 0, body, destPos, b.length);
                destPos += b.length;
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(), parseError(resp.body()));
            }

            Map<String, Object> respMap = json.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
            return ApiConfig.GATEWAY_HTTP + str(respMap.get("url")); // return full URL

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi upload file: " + e.getMessage(), e);
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
    
    private String str(Object o) {
        return o == null ? null : o.toString();
    }
}
