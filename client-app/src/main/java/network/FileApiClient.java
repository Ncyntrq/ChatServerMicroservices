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

    private Map<String, Object> doMultipartUpload(File file, Long channelId) throws Exception {
        String url = ApiConfig.GATEWAY_HTTP + "/api/files/upload";
        String token = SessionManager.get().getAccessToken();
        String userId = SessionManager.get().getUsername();
        if (token == null || userId == null) throw new ApiException("Chưa đăng nhập");

        String boundary = "Boundary-" + UUID.randomUUID().toString();
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String fileName = file.getName();
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";

        List<byte[]> byteArrays = new ArrayList<>();
        String separator = "--" + boundary + "\r\n";

        StringBuilder sb = new StringBuilder();
        // userId
        sb.append(separator);
        sb.append("Content-Disposition: form-data; name=\"userId\"\r\n\r\n");
        sb.append(userId).append("\r\n");
        // channelId (optional)
        if (channelId != null) {
            sb.append(separator);
            sb.append("Content-Disposition: form-data; name=\"channelId\"\r\n\r\n");
            sb.append(channelId).append("\r\n");
        }
        // file
        sb.append(separator);
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        sb.append("Content-Type: ").append(mimeType).append("\r\n\r\n");

        byteArrays.add(sb.toString().getBytes(StandardCharsets.UTF_8));
        byteArrays.add(fileBytes);
        byteArrays.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

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
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new ApiException(resp.statusCode(), parseError(resp.body()));
        }

        return json.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
    }

    public String uploadAvatar(File file) {
        try {
            Map<String, Object> respMap = doMultipartUpload(file, null);
            return ApiConfig.GATEWAY_HTTP + str(respMap.get("url"));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ApiException("Lỗi upload file: " + e.getMessage(), e);
        }
    }

    /** Kết quả upload file (URL đã đầy đủ qua gateway). */
    public static class Uploaded {
        public final long id;
        public final String url;          // full URL tải file gốc
        public final String thumbnailUrl; // full URL thumbnail (null nếu không phải ảnh)
        public final String name;
        public final String contentType;
        public final long size;

        Uploaded(long id, String url, String thumbnailUrl, String name, String contentType, long size) {
            this.id = id;
            this.url = url;
            this.thumbnailUrl = thumbnailUrl;
            this.name = name;
            this.contentType = contentType;
            this.size = size;
        }
    }

    /** Upload file đính kèm bất kỳ (max 10MB phía server), trả metadata. */
    public Uploaded uploadFile(File file, Long channelId) {
        try {
            String fileName = file.getName();
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) mimeType = "application/octet-stream";

            Map<String, Object> m = doMultipartUpload(file, channelId);

            String rawUrl = str(m.get("url"));
            String rawThumb = str(m.get("thumbnailUrl"));
            return new Uploaded(
                    asLong(m.get("id")),
                    rawUrl != null ? ApiConfig.GATEWAY_HTTP + rawUrl : null,
                    rawThumb != null ? ApiConfig.GATEWAY_HTTP + rawThumb : null,
                    str(m.get("originalName")) != null ? str(m.get("originalName")) : fileName,
                    str(m.get("contentType")) != null ? str(m.get("contentType")) : mimeType,
                    asLong(m.get("fileSize")));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ApiException("Lỗi upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra URL có trỏ đúng về API Gateway tin cậy không.
     * Chống lộ Bearer token: chỉ đính token cho host/scheme/port khớp gateway.
     */
    public static boolean isTrustedGatewayUrl(String urlString) {
        if (urlString == null) return false;
        try {
            URI u = URI.create(urlString);
            URI gw = URI.create(ApiConfig.GATEWAY_HTTP);
            if (u.getScheme() == null || u.getHost() == null) return false;
            return u.getScheme().equalsIgnoreCase(gw.getScheme())
                    && u.getHost().equalsIgnoreCase(gw.getHost())
                    && u.getPort() == gw.getPort();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tải bytes của 1 tài nguyên qua gateway. JWT gửi trong header Authorization,
     * KHÔNG bao giờ đặt token trong URL (tránh lộ qua log/history).
     * Từ chối nếu URL không trỏ về gateway tin cậy → chống exfiltration token.
     */
    public byte[] download(String fullUrl) {
        if (!isTrustedGatewayUrl(fullUrl)) {
            throw new ApiException("Từ chối tải từ host không tin cậy: " + fullUrl);
        }
        String token = SessionManager.get().getAccessToken();
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofSeconds(60))
                    .GET();
            if (token != null) b.header("Authorization", "Bearer " + token);
            HttpResponse<byte[]> resp = HttpClientHolder.get().send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(), "Tải file thất bại: HTTP " + resp.statusCode());
            }
            return resp.body();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ApiException("Lỗi tải file: " + e.getMessage(), e);
        }
    }

    /**
     * Liệt kê file đã upload vào 1 channel (cho sidebar Ảnh/Video & File).
     * @param type  "image" / "document" / null (tất cả)
     * @param limit số lượng tối đa
     * @return danh sách map metadata (id, originalName, contentType, fileSize, url, thumbnailUrl, createdAt).
     *         url/thumbnailUrl là đường dẫn TƯƠNG ĐỐI từ server — caller tự ghép gateway nếu cần tải.
     */
    public List<Map<String, Object>> getFilesByChannel(long channelId, String type, int limit) {
        try {
            StringBuilder url = new StringBuilder(ApiConfig.GATEWAY_HTTP)
                    .append("/api/files/channel/").append(channelId).append("?limit=").append(limit);
            if (type != null && !type.isBlank()) url.append("&type=").append(type);

            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .GET();
            String token = SessionManager.get().getAccessToken();
            if (token != null) b.header("Authorization", "Bearer " + token);

            HttpResponse<String> resp = HttpClientHolder.get().send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(), parseError(resp.body()));
            }
            return json.readValue(resp.body(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ApiException("Lỗi tải danh sách file: " + e.getMessage(), e);
        }
    }

    private long asLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        try { return o == null ? 0L : Long.parseLong(o.toString()); }
        catch (Exception e) { return 0L; }
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
