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
import javafx.scene.control.Hyperlink;
import javafx.stage.FileChooser;

import java.util.Locale;


public class ChatView extends BorderPane {

    private static final String BASE_URL = "http://localhost:8081";
    private static final String DEFAULT_AVATAR_URL = "/avatars/cat.png";

    private java.io.File pendingFile = null;
    private final Label pendingFileLabel = new Label("");
    private final Button clearFileBtn = new Button("✕");
    private final Button attachBtn = new Button("📎");

    private final DesktopClient client;
    private final UserToken user;

    private final Runnable onLogout;

    private final TextField input = new TextField();

    private Button dmBtn;

    // Bubble UI
    private final VBox messagesBox = new VBox(8);
    private final ScrollPane scrollPane = new ScrollPane(messagesBox);

    private Timeline queuePoller;
    private Timeline historyPoller;

    private Timeline dmUnreadPoller;
    private final Label dmBadge = new Label(""); // červená tečka / číslo

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

        dmBtn = new Button("DMs");
        dmBtn.setOnAction(e -> openDirectMessages());
        dmBtn.setStyle("""
           -fx-background-color: transparent;
           -fx-border-color: #d1d5db;
           -fx-border-radius: 6;
           -fx-padding: 6 14;""");

        // ✅ badge styling (number)
        dmBadge.setVisible(false);

        // 🔥 DŮLEŽITÉ: badge musí být managed=true, jinak ho StackPane nelayoutuje
        dmBadge.setManaged(true);

        // aby badge neblokoval kliknutí na tlačítko
        dmBadge.setMouseTransparent(true);

        dmBadge.setAlignment(Pos.CENTER);
        dmBadge.setStyle("""
         -fx-background-color: #ef4444;
         -fx-text-fill: white;
         -fx-font-size: 10px;
         -fx-font-weight: bold;
         -fx-background-radius: 999;""");

        // výchozí velikost pro 1 číslici
        dmBadge.setMinSize(16, 16);
        dmBadge.setPrefSize(16, 16);

        // StackPane: badge do pravého horního rohu tlačítka
        StackPane dmButtonWithBadge = new StackPane(dmBtn, dmBadge);
        StackPane.setAlignment(dmBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(dmBadge, new Insets(-6, -6, 0, 0));

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
                dmButtonWithBadge,
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

        attachBtn.setOnAction(e -> chooseFile());
        attachBtn.setStyle("-fx-padding: 6 10;");

        pendingFileLabel.setStyle("-fx-opacity: 0.75;");
        pendingFileLabel.setMaxWidth(240);
        pendingFileLabel.setWrapText(false);

        clearFileBtn.setOnAction(e -> clearPendingFile());
        clearFileBtn.setDisable(true);

        HBox fileChip = new HBox(6, pendingFileLabel, clearFileBtn);
        fileChip.setAlignment(Pos.CENTER_LEFT);

        send.setOnAction(e -> sendMessage());
        input.setOnAction(e -> sendMessage());

        VBox leftControls = new VBox(6, fileChip);
        leftControls.setAlignment(Pos.CENTER_LEFT);

        HBox bottom = new HBox(8, attachBtn, input, send, fileChip);
        bottom.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(input, Priority.ALWAYS);
        bottom.setPadding(new Insets(10, 0, 0, 0));
        setBottom(bottom);

        // Načti avatar přihlášeného usera z backendu (aby seděl i s webem)
        loadMyAvatarFromBackend();

        // Polling (queue = offline, history = online)
        startQueuePolling();
        startHistoryPolling();
        startDmUnreadPolling();

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
        if (dmUnreadPoller != null) {
            dmUnreadPoller.stop();
            dmUnreadPoller = null;
        }
    }

    private void sendMessage() {
        String text = input.getText().trim();
        if (text.isEmpty() && pendingFile == null) return;

        input.clear();

        // text
        if (!text.isEmpty()) {
            new Thread(() -> {
                try {
                    client.sendMessage(user, text);
                } catch (Exception ex) {
                    System.out.println("Send error: " + ex.getMessage());
                }
            }).start();
        }

        // file
        if (pendingFile != null) {
            java.io.File fileToSend = pendingFile;
            clearPendingFile();

            new Thread(() -> {
                try {
                    PayloadMessage uploaded = client.uploadFile(
                            user.getUsername(),
                            "1",     // public room id
                            null,    // receiverName null => public
                            fileToSend.toPath()
                    );

                } catch (Exception ex) {
                    System.out.println("Upload error: " + ex.getMessage());
                }
            }).start();
        }
    }

    private void startQueuePolling() {
        queuePoller = new Timeline(new KeyFrame(Duration.millis(300), e -> {
            new Thread(() -> {
                try {
                    List<PayloadMessage> list =
                            client.pickupMessages(user.getUserId().toString());
                    if (list != null && !list.isEmpty()) {
                        Platform.runLater(() -> {
                            boolean stick = isNearBottom();
                            for (var m : list) appendMessage(m);
                            if (stick) scrollToBottom();
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
                            boolean stick = isNearBottom();

                            if (list.size() < lastHistorySize) lastHistorySize = 0;
                            for (int i = lastHistorySize; i < list.size(); i++) {
                                appendMessage(list.get(i));
                            }
                            lastHistorySize = list.size();

                            if (stick) scrollToBottom();
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

    private void startDmUnreadPolling() {
        if (dmUnreadPoller != null) dmUnreadPoller.stop();

        dmUnreadPoller = new Timeline(new KeyFrame(Duration.millis(300), e -> {
            new Thread(() -> {
                try {
                    List<PayloadMessage> unread = client.getUnreadDirectMessages(user.getUsername());
                    int count = (unread == null) ? 0 : unread.size();

                    Platform.runLater(() -> updateDmBadge(count));
                } catch (Exception ex) {
                    System.out.println("DM badge poll error: " + ex.getMessage());
                }
            }).start();
        }));

        dmUnreadPoller.setCycleCount(Timeline.INDEFINITE);
        dmUnreadPoller.play();
    }

    private void updateDmBadge(int count) {
        // tlačítko vždy bez čísla
        dmBtn.setText("DMs");

        if (count <= 0) {
            dmBadge.setVisible(false);
            dmBadge.setText("");
            // managed necháváme TRUE pořád – StackPane to zvládne a je to stabilní
            return;
        }

        dmBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        dmBadge.setVisible(true);

        // velikost badge podle délky textu
        if (count > 9) {
            dmBadge.setMinSize(24, 16);
            dmBadge.setPrefSize(24, 16);
        } else {
            dmBadge.setMinSize(16, 16);
            dmBadge.setPrefSize(16, 16);
        }
    }

    private boolean isNearBottom() {
        return scrollPane.getVvalue() > 0.92;
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

        javafx.scene.Node bodyNode;

        FileInfo fi = extractFileInfo(msg);
        if (fi != null) {
            if (fi.isImage()) {
                ImageView preview = new ImageView(new Image(fi.url(), 0, 200, true, true));
                preview.setPreserveRatio(true);
                preview.setSmooth(true);
                preview.setOnMouseClicked(e -> client.openInBrowser(fi.url()));

                Label fn = new Label(fi.name());
                fn.setStyle("-fx-font-size: 11px; -fx-opacity: 0.75;");

                VBox box = new VBox(6, preview, fn);
                bodyNode = box;
            } else {
                Hyperlink link = new Hyperlink(fi.name());
                link.setOnAction(e -> client.openInBrowser(fi.url()));

                Label hint = new Label(fi.url());
                hint.setStyle("-fx-font-size: 10px; -fx-opacity: 0.6;");

                VBox box = new VBox(4, link, hint);
                bodyNode = box;
            }
        } else {
            Label body = new Label(content);
            body.setWrapText(true);
            body.setStyle("-fx-font-size: 13px;");
            bodyNode = body;
        }

        VBox bubble = new VBox(3, meta, bodyNode);
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
        String initial = (cached != null && !cached.isBlank()) ? cached : DEFAULT_AVATAR_URL;
        msgAvatar.setImage(new Image(BASE_URL + initial, 24, 24, true, true));

// ✅ vždy na pozadí ověř, jestli se avatar nezměnil (např. změna z webu)
        new Thread(() -> {
            try {
                String fresh = client.getAvatarUrlByUsername(sender);
                if (fresh == null || fresh.isBlank()) fresh = DEFAULT_AVATAR_URL;

                String prev = avatarCache.get(sender);
                if (prev == null || !prev.equals(fresh)) {
                    avatarCache.put(sender, fresh);
                    String finalFresh = fresh;
                    Platform.runLater(() ->
                            msgAvatar.setImage(new Image(BASE_URL + finalFresh, 24, 24, true, true))
                    );
                }
            } catch (Exception ignored) {}
        }).start();

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

    private record FileInfo(boolean isImage, String url, String name) {}

    private FileInfo extractFileInfo(PayloadMessage msg) {
        String fileUrl = safe(msg.getFileUrl());
        String fileName = safe(msg.getFileName());
        String type = safe(msg.getMessageType()).toUpperCase(Locale.ROOT);

        // 1) moderní forma (fileUrl v payload)
        if (!fileUrl.isBlank()) {
            boolean isImage = "IMAGE".equals(type) || looksLikeImage(fileName, fileUrl);
            return new FileInfo(isImage, client.fullUrl(fileUrl), fileName.isBlank() ? "file" : fileName);
        }

        // 2) fallback z historie: content = "[FILE] name | /uploads/.."
        String content = safe(msg.getContent());
        if (content.startsWith("[FILE]")) {
            String rest = content.substring(6).trim();
            String[] parts = rest.split("\\|");
            if (parts.length >= 2) {
                String name = parts[0].trim();
                String url = parts[1].trim();
                boolean isImage = looksLikeImage(name, url);
                return new FileInfo(isImage, client.fullUrl(url), name.isBlank() ? "file" : name);
            }
        }

        return null;
    }

    private boolean looksLikeImage(String name, String url) {
        String s = (name + " " + url).toLowerCase(Locale.ROOT);
        return s.endsWith(".png") || s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".gif") || s.endsWith(".webp") || s.endsWith(".bmp");
    }

    // ===== Avatar helpers =====

    private ImageView createHeaderAvatar(String avatarUrl) {
        ImageView avatar = new ImageView(new Image(BASE_URL + avatarUrl, 28, 28, true, true));
        avatar.setFitWidth(28);
        avatar.setFitHeight(28);
        avatar.setPreserveRatio(true);
        avatar.setSmooth(true);

        // ✅ circle clip
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(14, 14, 14);
        avatar.setClip(clip);

        avatar.setStyle("-fx-cursor: hand;");
        return avatar;
    }

    private ImageView createMessageAvatar(String avatarUrl) {
        ImageView avatar = new ImageView(new Image(BASE_URL + avatarUrl, 24, 24, true, true));
        avatar.setFitWidth(24);
        avatar.setFitHeight(24);
        avatar.setPreserveRatio(true);
        avatar.setSmooth(true);

        // ✅ circle clip
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(12, 12, 12);
        avatar.setClip(clip);

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

    private void openDirectMessages() {
        // ✅ jen lokálně skryj badge – server nemarkuj, ať DM okno vidí unread u userů
        updateDmBadge(0);

        Stage owner = (Stage) getScene().getWindow();
        Stage dmStage = new Stage();
        dmStage.initOwner(owner);
        dmStage.setTitle("Direct Messages · " + user.getUsername());

        DirectMessagesView dmView = new DirectMessagesView(client, user, avatarCache);

        dmStage.setScene(new javafx.scene.Scene(dmView, 980, 640));
        dmStage.show();
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

    private void chooseFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select a file");
        java.io.File f = fc.showOpenDialog(getScene().getWindow());
        if (f == null) return;

        pendingFile = f;
        pendingFileLabel.setText(f.getName());
        clearFileBtn.setDisable(false);
    }

    private void clearPendingFile() {
        pendingFile = null;
        pendingFileLabel.setText("");
        clearFileBtn.setDisable(true);
    }
}
