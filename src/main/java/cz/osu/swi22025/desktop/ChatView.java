package cz.osu.swi22025.desktop;

import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.model.json.UserToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatView extends BorderPane {

    private static final String BASE_URL = "http://localhost:8081";
    private static final String DEFAULT_AVATAR_URL = "/avatars/cat.png";

    private final DesktopClient client;
    private final UserToken user;

    private final Runnable onLogout;

    private final TextField input = new TextField();

    // Bubble UI
    private final VBox messagesBox = new VBox(8);
    private final ScrollPane scrollPane = new ScrollPane(messagesBox);

    private Timeline queuePoller;
    private Timeline historyPoller;

    // Dedupe (history + queue mohou vracet stejné zprávy)
    private final Set<String> seen = new HashSet<>();

    private int lastHistorySize = 0;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Avatars
    private final AvatarCache avatarCache = new AvatarCache();
    private String currentAvatarUrl = DEFAULT_AVATAR_URL;
    private ImageView headerAvatarView;

    public ChatView(DesktopClient client, UserToken user, Runnable onLogout) {
        this.client = client;
        this.user = user;
        this.onLogout = onLogout;

        setPadding(new Insets(12));

        // ===== HEADER =====
        Label title = new Label("Hlavní místnost · " + user.getUsername());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label statusDot = new Label("●");
        statusDot.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 14px;");

        Label statusText = new Label("Connected");
        statusText.setStyle("-fx-text-fill: #374151;");

        HBox statusBox = new HBox(6, statusDot, statusText);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        // Avatar vedle LOGOUT (klikací)
        headerAvatarView = createHeaderAvatar(DEFAULT_AVATAR_URL);
        headerAvatarView.setOnMouseClicked(e -> openAvatarPicker());

        Button logoutBtn = new Button("LOGOUT");
        logoutBtn.setStyle("""
         -fx-background-color: transparent;
         -fx-border-color: #d1d5db;
         -fx-border-radius: 6;
         -fx-padding: 6 14;""");

        logoutBtn.setOnAction(e -> {
            stopPolling();          // ⛔ zastaví history + queue polling
            avatarCache.clear();    // pro jistotu
            if (onLogout != null) onLogout.run(); // ↩️ zpět na LoginView
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(16,
                title,
                spacer,
                statusBox,
                headerAvatarView,
                logoutBtn
        );
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 14, 10, 14));
        header.setStyle("""
         -fx-background-color: #f9fafb;
         -fx-border-color: #e5e7eb;
         -fx-border-width: 0 0 1 0;""");

        setTop(header);
        BorderPane.setMargin(header, new Insets(0, 0, 10, 0));
        // ===== END HEADER =====

        // Scroll
        messagesBox.setPadding(new Insets(10));
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: #f6f7f9; -fx-background-color: #f6f7f9;");
        setCenter(scrollPane);

        // Input bar
        input.setPromptText("Napiš zprávu…");
        Button send = new Button("Send");

        send.setOnAction(e -> sendMessage());
        input.setOnAction(e -> sendMessage());

        HBox bottom = new HBox(8, input, send);
        bottom.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(input, Priority.ALWAYS);
        bottom.setPadding(new Insets(10, 0, 0, 0));
        setBottom(bottom);

        // Načti avatar přihlášeného usera z backendu (aby seděl i s webem)
        loadMyAvatarFromBackend();

        // Polling (queue = offline, history = online)
        startQueuePolling();
        startHistoryPolling();

        // Stop timers when window closes
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o2, oldW, newW) -> {
                    if (newW != null) {
                        newW.setOnHidden(e -> stopPolling());
                    }
                });
            }
        });
    }

    private void stopPolling() {
        if (queuePoller != null) {
            queuePoller.stop();
            queuePoller = null;
        }
        if (historyPoller != null) {
            historyPoller.stop();
            historyPoller = null;
        }
    }

    private void sendMessage() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;

        input.clear();

        // Nezobrazujeme lokálně, aby nevznikaly duplicity.
        // Zpráva se objeví z /api/history během chvilky.
        new Thread(() -> {
            try {
                client.sendMessage(user, text);
            } catch (Exception ex) {
                System.out.println("Send error: " + ex.getMessage());
            }
        }).start();
    }

    private void startQueuePolling() {
        queuePoller = new Timeline(new KeyFrame(Duration.millis(300), e -> {
            new Thread(() -> {
                try {
                    List<PayloadMessage> list =
                            client.pickupMessages(user.getUserId().toString());
                    if (list != null && !list.isEmpty()) {
                        Platform.runLater(() -> {
                            for (var m : list) appendMessage(m);
                            scrollToBottom();
                        });
                    }
                } catch (Exception ex) {
                    System.out.println("Queue polling error: " + ex.getMessage());
                }
            }).start();
        }));
        queuePoller.setCycleCount(Timeline.INDEFINITE);
        queuePoller.play();
    }

    private void startHistoryPolling() {
        historyPoller = new Timeline(new KeyFrame(Duration.millis(300), e -> {
            new Thread(() -> {
                try {
                    List<PayloadMessage> list = client.getHistory("1");
                    if (list != null && !list.isEmpty()) {
                        Platform.runLater(() -> {
                            // pokud by se historie restartla / zkrátila, začni od nuly
                            if (list.size() < lastHistorySize) {
                                lastHistorySize = 0;
                            }

                            // přidej jen nové zprávy, které ještě nebyly
                            for (int i = lastHistorySize; i < list.size(); i++) {
                                appendMessage(list.get(i));
                            }

                            lastHistorySize = list.size();
                            scrollToBottom();
                        });
                    }
                } catch (Exception ex) {
                    System.out.println("History polling error: " + ex.getMessage());
                }
            }).start();
        }));
        historyPoller.setCycleCount(Timeline.INDEFINITE);
        historyPoller.play();
    }

    private void scrollToBottom() {
        // scroll after layout pass
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void appendMessage(PayloadMessage msg) {
        if (msg == null) return;

        // robust fields
        String room = safe(msg.getReceiverChatRoomId());
        String sender = safe(msg.getSenderName());
        String content = safe(msg.getContent());
        String date = msg.getDate();

        // Dedupe key: room + sender + content + normalized time bucket (seconds)
        String timeBucket = normalizeToSecond(date);
        String key = room + "|" + sender + "|" + content + "|" + timeBucket;

        if (seen.contains(key)) return;
        seen.add(key);

        boolean mine = sender.equals(user.getUsername());

        // Time formatting (robust)
        String timeText = formatTime(date);

        // Bubble components
        Label meta = new Label(timeText + "  •  " + (sender.isBlank() ? "UNKNOWN" : sender));
        meta.setStyle("-fx-font-size: 11px; -fx-opacity: 0.75;");

        Label body = new Label(content);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 13px;");

        VBox bubble = new VBox(3, meta, body);
        bubble.setPadding(new Insets(8, 10, 8, 10));
        bubble.setMaxWidth(520);

        if (mine) {
            bubble.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 14;");
        } else {
            bubble.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 14; -fx-border-color: #e5e7eb; -fx-border-radius: 14;");
        }

        // Avatar u zprávy (cache + async fetch)
        ImageView msgAvatar = createMessageAvatar(DEFAULT_AVATAR_URL);

        String cached = avatarCache.get(sender);
        if (cached != null && !cached.isBlank()) {
            msgAvatar.setImage(new Image(BASE_URL + cached, 24, 24, true, true));
        } else {
            // hned zobraz default a na pozadí dotáhni reálný
            new Thread(() -> {
                try {
                    String url = client.getAvatarUrlByUsername(sender);
                    avatarCache.put(sender, url);
                    Platform.runLater(() ->
                            msgAvatar.setImage(new Image(BASE_URL + url, 24, 24, true, true))
                    );
                } catch (Exception ignored) {}
            }).start();
        }

        HBox row;
        if (mine) {
            // moje zpráva: bubble + avatar vpravo
            row = new HBox(8, bubble, msgAvatar);
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            // cizí zpráva: avatar vlevo + bubble
            row = new HBox(8, msgAvatar, bubble);
            row.setAlignment(Pos.CENTER_LEFT);
        }

        row.setPadding(new Insets(2, 0, 2, 0));
        messagesBox.getChildren().add(row);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // ===== Avatar helpers =====

    private ImageView createHeaderAvatar(String avatarUrl) {
        ImageView avatar = new ImageView(new Image(BASE_URL + avatarUrl, 28, 28, true, true));
        avatar.setFitWidth(28);
        avatar.setFitHeight(28);
        avatar.setStyle("-fx-cursor: hand; -fx-background-radius: 999;");
        return avatar;
    }

    private ImageView createMessageAvatar(String avatarUrl) {
        ImageView avatar = new ImageView(new Image(BASE_URL + avatarUrl, 24, 24, true, true));
        avatar.setFitWidth(24);
        avatar.setFitHeight(24);
        avatar.setStyle("-fx-background-radius: 999;");
        return avatar;
    }

    private void loadMyAvatarFromBackend() {
        // načti avatar přihlášeného uživatele (aby seděl s webem)
        new Thread(() -> {
            try {
                String url = client.getAvatarUrlByUsername(user.getUsername());
                currentAvatarUrl = (url == null || url.isBlank()) ? DEFAULT_AVATAR_URL : url;
                avatarCache.put(user.getUsername(), currentAvatarUrl);

                Platform.runLater(() ->
                        headerAvatarView.setImage(new Image(BASE_URL + currentAvatarUrl, 28, 28, true, true))
                );
            } catch (Exception ignored) {}
        }).start();
    }

    private void openAvatarPicker() {
        Stage stage = (Stage) getScene().getWindow();
        String chosenFile = AvatarPickerDialog.show(stage, BASE_URL, currentAvatarUrl);
        if (chosenFile == null) return; // cancel

        new Thread(() -> {
            try {
                // uloží do DB -> projeví se i na webu
                String updatedUrl = client.updateAvatar(user.getUserId(), chosenFile);
                if (updatedUrl == null || updatedUrl.isBlank()) updatedUrl = DEFAULT_AVATAR_URL;

                currentAvatarUrl = updatedUrl;
                avatarCache.put(user.getUsername(), updatedUrl);

                String finalUpdatedUrl = updatedUrl;
                Platform.runLater(() ->
                        headerAvatarView.setImage(new Image(BASE_URL + finalUpdatedUrl, 28, 28, true, true))
                );
            } catch (Exception ex) {
                System.out.println("Avatar update failed: " + ex.getMessage());
            }
        }).start();
    }

    // ===== Timestamp helpers =====

    private String formatTime(String date) {
        ZonedDateTime zdt = parseAnyDate(date);
        return "[" + zdt.format(TIME_FMT) + "]";
    }

    private String normalizeToSecond(String date) {
        if (date == null || date.isBlank()) {
            return "no-time";
        }
        try {
            ZonedDateTime zdt = parseAnyDate(date);
            return zdt.withNano(0).toInstant().toString();
        } catch (Exception e) {
            return "no-time";
        }
    }

    private ZonedDateTime parseAnyDate(String date) {
        ZoneId zone = ZoneId.systemDefault();

        if (date == null || date.isBlank()) {
            return ZonedDateTime.now(zone);
        }

        String d = date.trim();

        try {
            return Instant.parse(d).atZone(zone);
        } catch (DateTimeParseException ignored) {}

        try {
            return OffsetDateTime.parse(d).atZoneSameInstant(zone);
        } catch (DateTimeParseException ignored) {}

        try {
            return ZonedDateTime.parse(d).withZoneSameInstant(zone);
        } catch (DateTimeParseException ignored) {}

        try {
            return LocalDateTime.parse(d).atZone(zone);
        } catch (DateTimeParseException ignored) {}

        return ZonedDateTime.now(zone);
    }
}
