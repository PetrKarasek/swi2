package cz.osu.swi22025.desktop;

import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.model.json.UserToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Desktop Direct Messages implementation compatible with the web app:
 * - REST send: POST /api/private-message
 * - REST history: GET /api/direct-history?user1&user2
 * - REST users: GET /users
 *
 * Works together with the web (STOMP) because backend broadcasts every DM
 * to /user/{name}/private, and also stores messages in DB.
 */
public class DirectMessagesView extends BorderPane {

    private static final String BASE_URL = "http://localhost:8081";
    private static final String DEFAULT_AVATAR_URL = "/avatars/cat.png";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DesktopClient client;
    private final UserToken me;
    private final AvatarCache avatarCache;

    // Left: user list
    private final ListView<String> usersList = new ListView<>();
    private final TextField search = new TextField();
    private final Label leftTitle = new Label("Users");

    // Center: messages
    private final VBox messagesBox = new VBox(8);
    private final ScrollPane scrollPane = new ScrollPane(messagesBox);
    private final Label chatTitle = new Label("Select a user to start a DM");

    private final TextField input = new TextField();
    private final Button sendBtn = new Button("Send");

    private String activePeer = null;

    // Polling history
    private Timeline dmHistoryPoller;
    private int lastHistorySize = 0;
    private final Set<String> seen = new HashSet<>();

    // Unread counters
    private Timeline unreadPoller;
    private final Map<String, Integer> unreadByUser = new HashMap<>();

    public DirectMessagesView(DesktopClient client, UserToken me, AvatarCache avatarCache) {
        this.client = client;
        this.me = me;
        this.avatarCache = avatarCache;

        setPadding(new Insets(12));

        // ===== LEFT SIDEBAR =====
        leftTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        search.setPromptText("Search user…");

        VBox left = new VBox(8, leftTitle, search, usersList);
        left.setPadding(new Insets(10));
        left.setPrefWidth(240);
        left.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-width: 0 1 0 0;");

        // Custom cell: avatar + username + unread badge
        usersList.setCellFactory(lv -> new ListCell<>() {
            private final HBox row = new HBox(10);
            private final ImageView av = new ImageView();
            private final Label name = new Label();
            private final Label badge = new Label();
            private final Region spacer = new Region();

            {
                av.setFitWidth(26);
                av.setFitHeight(26);
                av.setPreserveRatio(true);
                av.setSmooth(true);

                badge.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 2 7; -fx-background-radius: 999; -fx-font-size: 11px;");
                badge.setVisible(false);

                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.getChildren().addAll(av, name, spacer, badge);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                name.setText(item);

                // avatar from cache or backend
                String cached = avatarCache.get(item);
                String url = (cached == null || cached.isBlank()) ? DEFAULT_AVATAR_URL : cached;
                av.setImage(new Image(BASE_URL + url, 26, 26, true, true));

                if (cached == null) {
                    new Thread(() -> {
                        try {
                            String real = client.getAvatarUrlByUsername(item);
                            avatarCache.put(item, real);
                            Platform.runLater(() -> av.setImage(new Image(BASE_URL + real, 26, 26, true, true)));
                        } catch (Exception ignored) {}
                    }).start();
                }

                int unread = unreadByUser.getOrDefault(item, 0);
                badge.setText(String.valueOf(unread));
                badge.setVisible(unread > 0);

                setGraphic(row);
            }
        });

        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || newV.isBlank()) return;
            openConversation(newV);
        });

        // Search filter
        search.textProperty().addListener((obs, oldV, newV) -> loadUsers(newV == null ? "" : newV.trim()));

        setLeft(left);

        // ===== CENTER CHAT =====
        chatTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        messagesBox.setPadding(new Insets(10));
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: #ffffff; -fx-background-color: #ffffff;");

        input.setPromptText("Type a private message…");
        input.setDisable(true);
        sendBtn.setDisable(true);
        sendBtn.setOnAction(e -> sendDm());
        input.setOnAction(e -> sendDm());

        HBox bottom = new HBox(8, input, sendBtn);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10));
        HBox.setHgrow(input, Priority.ALWAYS);

        VBox center = new VBox(10, chatTitle, scrollPane, bottom);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        center.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");
        center.setPadding(new Insets(10));

        setCenter(center);
        BorderPane.setMargin(center, new Insets(0, 0, 0, 10));

        // Initial load
        loadUsers("");
        startUnreadPolling();

        // Cleanup when window closes
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o2, oldW, newW) -> {
                    if (newW != null) newW.setOnHidden(e -> stopPolling());
                });
            }
        });
    }

    private void stopPolling() {
        if (dmHistoryPoller != null) {
            dmHistoryPoller.stop();
            dmHistoryPoller = null;
        }
        if (unreadPoller != null) {
            unreadPoller.stop();
            unreadPoller = null;
        }
    }

    private void loadUsers(String filter) {
        new Thread(() -> {
            try {
                List<String> users = client.getAllUsers();
                users = users == null ? List.of() : users;

                String f = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
                List<String> filtered = users.stream()
                        .filter(u -> u != null && !u.equalsIgnoreCase(me.getUsername()))
                        .filter(u -> f.isBlank() || u.toLowerCase(Locale.ROOT).contains(f))
                        .sorted(String::compareToIgnoreCase)
                        .toList();

                Platform.runLater(() -> {
                    String selected = usersList.getSelectionModel().getSelectedItem();
                    usersList.getItems().setAll(filtered);
                    if (selected != null && filtered.contains(selected)) {
                        usersList.getSelectionModel().select(selected);
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void openConversation(String peer) {
        activePeer = peer;
        chatTitle.setText("Direct Message · " + me.getUsername() + " ↔ " + peer);
        input.setDisable(false);
        sendBtn.setDisable(false);

        // Clear unread for this peer in UI + mark read on server
        unreadByUser.put(peer, 0);
        usersList.refresh();
        new Thread(() -> {
            try { client.markDirectMessagesRead(me.getUsername()); } catch (Exception ignored) {}
        }).start();

        // Reset history
        lastHistorySize = 0;
        seen.clear();
        messagesBox.getChildren().clear();

        // Start polling this conversation
        startHistoryPolling(peer);
    }

    private void startHistoryPolling(String peer) {
        if (dmHistoryPoller != null) dmHistoryPoller.stop();

        dmHistoryPoller = new Timeline(new KeyFrame(Duration.millis(350), e -> {
            new Thread(() -> {
                try {
                    if (activePeer == null || !activePeer.equals(peer)) return;

                    List<PayloadMessage> list = client.getDirectHistory(me.getUsername(), peer);
                    if (list == null) list = List.of();

                    List<PayloadMessage> finalList = list;
                    Platform.runLater(() -> {
                        if (finalList.size() < lastHistorySize) lastHistorySize = 0;

                        for (int i = lastHistorySize; i < finalList.size(); i++) {
                            appendMessage(finalList.get(i));
                        }

                        lastHistorySize = finalList.size();
                        scrollToBottom();
                    });
                } catch (Exception ignored) {}
            }).start();
        }));

        dmHistoryPoller.setCycleCount(Timeline.INDEFINITE);
        dmHistoryPoller.play();
    }

    private void startUnreadPolling() {
        unreadPoller = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            new Thread(() -> {
                try {
                    List<PayloadMessage> unread = client.getUnreadDirectMessages(me.getUsername());
                    if (unread == null) unread = List.of();

                    Map<String, Integer> counts = new HashMap<>();
                    for (PayloadMessage m : unread) {
                        if (m == null) continue;
                        String sender = safe(m.getSenderName());
                        if (sender.isBlank() || sender.equalsIgnoreCase(me.getUsername())) continue;
                        counts.put(sender, counts.getOrDefault(sender, 0) + 1);
                    }

                    Platform.runLater(() -> {
                        unreadByUser.clear();
                        unreadByUser.putAll(counts);

                        // If we are currently chatting with someone, keep it 0 locally
                        if (activePeer != null) unreadByUser.put(activePeer, 0);
                        usersList.refresh();
                    });
                } catch (Exception ignored) {}
            }).start();
        }));

        unreadPoller.setCycleCount(Timeline.INDEFINITE);
        unreadPoller.play();
    }

    private void sendDm() {
        if (activePeer == null || activePeer.isBlank()) return;
        String text = input.getText().trim();
        if (text.isEmpty()) return;

        input.clear();

        new Thread(() -> {
            try {
                client.sendDirectMessage(me, activePeer, text);
            } catch (Exception ignored) {}
        }).start();
    }

    private void appendMessage(PayloadMessage msg) {
        if (msg == null) return;

        String sender = safe(msg.getSenderName());
        String receiver = safe(msg.getReceiverName());
        String content = safe(msg.getContent());
        String date = msg.getDate();

        // Accept only messages from this conversation
        if (activePeer == null) return;
        boolean isBetween = (sender.equalsIgnoreCase(me.getUsername()) && receiver.equalsIgnoreCase(activePeer))
                || (sender.equalsIgnoreCase(activePeer) && receiver.equalsIgnoreCase(me.getUsername()));
        if (!isBetween) return;

        String key = sender + "|" + receiver + "|" + content + "|" + normalizeToSecond(date);
        if (seen.contains(key)) return;
        seen.add(key);

        boolean mine = sender.equalsIgnoreCase(me.getUsername());

        String timeText = formatTime(date);
        Label meta = new Label(timeText + "  •  " + sender);
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

        // Avatar: payload senderAvatarUrl -> cache -> backend
        ImageView msgAvatar = new ImageView();
        msgAvatar.setFitWidth(24);
        msgAvatar.setFitHeight(24);
        msgAvatar.setPreserveRatio(true);
        msgAvatar.setSmooth(true);

        String avatarUrl = safe(msg.getSenderAvatarUrl());
        if (avatarUrl.isBlank()) {
            String cached = avatarCache.get(sender);
            avatarUrl = (cached == null || cached.isBlank()) ? DEFAULT_AVATAR_URL : cached;
        } else {
            avatarCache.put(sender, avatarUrl);
        }

        msgAvatar.setImage(new Image(BASE_URL + avatarUrl, 24, 24, true, true));

        if (msg.getSenderAvatarUrl() == null) {
            new Thread(() -> {
                try {
                    String real = client.getAvatarUrlByUsername(sender);
                    avatarCache.put(sender, real);
                    Platform.runLater(() -> msgAvatar.setImage(new Image(BASE_URL + real, 24, 24, true, true)));
                } catch (Exception ignored) {}
            }).start();
        }

        HBox row;
        if (mine) {
            row = new HBox(8, bubble, msgAvatar);
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row = new HBox(8, msgAvatar, bubble);
            row.setAlignment(Pos.CENTER_LEFT);
        }
        row.setPadding(new Insets(2, 0, 2, 0));
        messagesBox.getChildren().add(row);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String formatTime(String date) {
        ZonedDateTime zdt = parseAnyDate(date);
        return "[" + zdt.format(TIME_FMT) + "]";
    }

    private String normalizeToSecond(String date) {
        if (date == null || date.isBlank()) return "no-time";
        try {
            ZonedDateTime zdt = parseAnyDate(date);
            return zdt.withNano(0).toInstant().toString();
        } catch (Exception e) {
            return "no-time";
        }
    }

    private ZonedDateTime parseAnyDate(String date) {
        ZoneId zone = ZoneId.systemDefault();
        if (date == null || date.isBlank()) return ZonedDateTime.now(zone);
        String d = date.trim();

        try { return Instant.parse(d).atZone(zone); } catch (DateTimeParseException ignored) {}
        try { return OffsetDateTime.parse(d).atZoneSameInstant(zone); } catch (DateTimeParseException ignored) {}
        try { return ZonedDateTime.parse(d).withZoneSameInstant(zone); } catch (DateTimeParseException ignored) {}
        try { return LocalDateTime.parse(d).atZone(zone); } catch (DateTimeParseException ignored) {}

        return ZonedDateTime.now(zone);
    }
}
