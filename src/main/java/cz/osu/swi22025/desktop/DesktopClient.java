package cz.osu.swi22025.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.model.json.UserToken;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DesktopClient {

    private static final String BASE_URL = "http://localhost:8081";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== AUTH =====

    public UserToken login(String username, String password)
            throws IOException, InterruptedException {

        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", password)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Login failed" : response.body());
        }

        return objectMapper.readValue(response.body(), UserToken.class);
    }

    public UserToken register(String username, String password)
            throws IOException, InterruptedException {

        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", password)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/signup"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // ✅ DŮLEŽITÉ: když backend vrátí 409 (už existuje), NESMÍME to ignorovat
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Signup failed" : response.body());
        }

        // po signup se rovnou přihlásíme
        return login(username, password);
    }

    // ===== SEND MESSAGE =====

    public void sendMessage(UserToken user, String content)
            throws IOException, InterruptedException {

        PayloadMessage msg = new PayloadMessage();
        msg.setSenderName(user.getUsername());
        msg.setReceiverChatRoomId("1");
        msg.setContent(content);
        msg.setDate(java.time.Instant.now().toString());

        String body = objectMapper.writeValueAsString(msg);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/message"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ===== QUEUE (OFFLINE) =====

    public List<PayloadMessage> pickupMessages(String userId)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/queue?userId=" + userId))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(
                response.body(),
                new TypeReference<List<PayloadMessage>>() {}
        );
    }

    // ===== HISTORY (ONLINE USERS) =====

    public List<PayloadMessage> getHistory(String chatRoomId)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/history?chatRoomId=" + chatRoomId))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(
                response.body(),
                new TypeReference<List<PayloadMessage>>() {}
        );
    }

    public String getAvatarUrlByUsername(String username) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/users/by-username/" + username))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Failed to fetch profile" : response.body());
        }

        // očekává JSON: { "username":"...", "avatarUrl":"/avatars/cat.png" }
        Map<String, Object> map = objectMapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        Object avatarUrl = map.get("avatarUrl");
        return avatarUrl == null ? "/avatars/cat.png" : avatarUrl.toString();
    }

    public String updateAvatar(UUID userId, String avatarFile) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of("avatarFile", avatarFile));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/users/" + userId + "/avatar"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Failed to update avatar" : response.body());
        }

        Map<String, Object> map = objectMapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        Object avatarUrl = map.get("avatarUrl");
        return avatarUrl == null ? "/avatars/cat.png" : avatarUrl.toString();
    }

}
