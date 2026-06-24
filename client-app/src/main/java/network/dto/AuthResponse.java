package network.dto;

public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;

    public AuthResponse() {
    }

    // Backward-compatible constructor (token + username only)
    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }

    public AuthResponse(String token, String refreshToken, String username) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
