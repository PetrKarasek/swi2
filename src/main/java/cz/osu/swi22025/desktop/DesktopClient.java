package cz.osu.swi22025.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.model.json.UserToken;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DesktopClient {

    public static final String BASE_URL = "http://localhost:8081";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== AUTH =====

    public UserToken login(String username, String password) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Login failed" : response.body());
        }

        return objectMapper.readValue(response.body(), UserToken.class);
    }

    public UserToken register(String username, String password) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/signup"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Signup failed" : response.body());
        }

        return login(username, password);
    }

    // ===== PUBLIC MESSAGES =====

    public void sendMessage(UserToken user, String content) throws IOException, InterruptedException {
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

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Send failed" : response.body());
        }
    }

    public List<PayloadMessage> pickupMessages(String userId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/queue?userId=" + URLEncoder.encode(userId, StandardCharsets.UTF_8)))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Queue pickup failed" : response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<PayloadMessage>>() {});
    }

    public List<PayloadMessage> getHistory(String chatRoomId) throws IOException, InterruptedException {
        String url = BASE_URL + "/api/history?chatRoomId=" + URLEncoder.encode(chatRoomId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "History failed" : response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<PayloadMessage>>() {});
    }

    // ===== AVATARS =====

    public String getAvatarUrlByUsername(String username) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/users/by-username/" + URLEncoder.encode(username, StandardCharsets.UTF_8)))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            return "/avatars/cat.png";
        }

        Map<String, Object> map = objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
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

        Map<String, Object> map = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        Object avatarUrl = map.get("avatarUrl");
        return avatarUrl == null ? "/avatars/cat.png" : avatarUrl.toString();
    }

    public List<String> getAllUsers() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Failed to fetch users" : response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<String>>() {});
    }

    // ===== DIRECT MESSAGES =====

    public void sendDirectMessage(UserToken user, String receiverName, String content)
            throws IOException, InterruptedException {

        PayloadMessage msg = new PayloadMessage();
        msg.setSenderName(user.getUsername());
        msg.setReceiverName(receiverName);
        msg.setReceiverChatRoomId("");
        msg.setContent(content);
        msg.setDate(java.time.Instant.now().toString());

        String body = objectMapper.writeValueAsString(msg);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/private-message"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "DM send failed" : response.body());
        }
    }

    public List<PayloadMessage> getDirectHistory(String user1, String user2)
            throws IOException, InterruptedException {

        String url = BASE_URL + "/api/direct-history?user1="
                + URLEncoder.encode(user1, StandardCharsets.UTF_8)
                + "&user2="
                + URLEncoder.encode(user2, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Failed to fetch DM history" : response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<PayloadMessage>>() {});
    }

    public List<PayloadMessage> getUnreadDirectMessages(String username)
            throws IOException, InterruptedException {

        String url = BASE_URL + "/api/unread-messages?username="
                + URLEncoder.encode(username, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Failed to fetch unread DMs" : response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<PayloadMessage>>() {});
    }

    public void markDirectMessagesRead(String username) throws IOException, InterruptedException {
        String url = BASE_URL + "/api/mark-messages-read?username="
                + URLEncoder.encode(username, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException(response.body() == null ? "Failed to mark DMs as read" : response.body());
        }
    }

    // ===== FILE UPLOAD (PUBLIC + DM) =====
    // backend: POST /api/upload-file multipart:
    // username, receiverChatRoomId, receiverName (optional), id (optional), file

    public PayloadMessage uploadFile(String username,
                                     String receiverChatRoomId,
                                     String receiverNameOrNull,
                                     Path filePath) throws Exception {

        String boundary = "----SWI2Boundary" + UUID.randomUUID();
        List<byte[]> parts = new ArrayList<>();

        addTextPart(parts, boundary, "username", username);
        addTextPart(parts, boundary, "receiverChatRoomId", receiverChatRoomId == null ? "" : receiverChatRoomId);

        if (receiverNameOrNull != null && !receiverNameOrNull.isBlank()) {
            addTextPart(parts, boundary, "receiverName", receiverNameOrNull);
        }

        // id (optional) – ale je fajn pro dedupe
        addTextPart(parts, boundary, "id", UUID.randomUUID().toString());

        // file
        String filename = filePath.getFileName().toString();
        String contentType = guessContentType(filename);

        parts.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(Files.readAllBytes(filePath));
        parts.add("\r\n".getBytes(StandardCharsets.UTF_8));

        parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/upload-file"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(parts))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Upload failed: " + response.statusCode() + " " + response.body());
        }

        return objectMapper.readValue(response.body(), PayloadMessage.class);
    }

    private void addTextPart(List<byte[]> out, String boundary, String name, String value) {
        out.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.add(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.add((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        out.add("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String guessContentType(String filename) {
        String f = filename.toLowerCase(Locale.ROOT);
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".gif")) return "image/gif";
        if (f.endsWith(".webp")) return "image/webp";
        if (f.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    public String fullUrl(String maybeRelativeUrl) {
        if (maybeRelativeUrl == null || maybeRelativeUrl.isBlank()) return "";
        if (maybeRelativeUrl.startsWith("http://") || maybeRelativeUrl.startsWith("https://")) return maybeRelativeUrl;
        if (maybeRelativeUrl.startsWith("/")) return BASE_URL + maybeRelativeUrl;
        return BASE_URL + "/" + maybeRelativeUrl;
    }

    public void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {}
    }
}