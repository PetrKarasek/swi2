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

public class DesktopClient {

    private static final String BASE_URL = "http://localhost:8081";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DesktopClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // =========================
    // AUTH
    // =========================

    public UserToken login(String username, String password)
            throws IOException, InterruptedException {

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "username", username,
                        "password", password
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Login failed: "
                    + response.statusCode() + " - " + response.body());
        }

        return objectMapper.readValue(response.body(), UserToken.class);
    }

    public UserToken register(String username, String password)
            throws IOException, InterruptedException {

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "username", username,
                        "password", password
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/signup"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Register failed: "
                    + response.statusCode() + " - " + response.body());
        }

        // ✅ Pokud backend vrací JSON token, zkusíme ho parsovat
        String respBody = response.body() == null ? "" : response.body().trim();
        if (respBody.startsWith("{") || respBody.startsWith("[")) {
            try {
                return objectMapper.readValue(respBody, UserToken.class);
            } catch (Exception ignored) {
                // spadneme na login fallback
            }
        }

        // ✅ Fallback: backend vrátil text (např. "User created"), tak se hned přihlásíme
        return login(username, password);
    }

    // =========================
    // CHAT – SEND MESSAGE
    // =========================

    public void sendMessage(UserToken user, String content)
            throws IOException, InterruptedException {

        PayloadMessage payload = new PayloadMessage();
        payload.setSenderName(user.getUsername());
        payload.setReceiverChatRoomId("1"); // hlavní místnost
        payload.setContent(content);
        payload.setDate(java.time.Instant.now().toString());

        String body = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/message"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Send message failed: "
                    + response.statusCode() + " - " + response.body());
        }
    }

    // =========================
    // CHAT – PICKUP QUEUE
    // =========================

    public List<PayloadMessage> pickupMessages(String userId)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/queue?userId=" + userId))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<PayloadMessage>>() {}
            );
        }

        throw new IOException("Pickup messages failed: "
                + response.statusCode() + " - " + response.body());
    }
}
