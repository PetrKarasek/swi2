package cz.osu.swi22025.desktop;

import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.model.json.UserToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main chat room view for the desktop client.
 * Desktop klient nepoužívá WebSocket – pro "real-time" používá polling /api/queue.
 */
public class ChatView extends BorderPane {

    private final DesktopClient client;
    private final UserToken user;

    private final TextArea messagesArea;
    private final TextField inputField;

    private Timeline queuePoller;
    private volatile boolean polling = false;
    private volatile boolean pollingRequestInFlight = false;

    public ChatView(DesktopClient client, UserToken user) {
        this.client = client;
        this.user = user;

        setPadding(new Insets(10));

        Label header = new Label("Hlavní místnost – " + user.getUsername());
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        setTop(header);
        BorderPane.setAlignment(header, Pos.CENTER_LEFT);

        messagesArea = new TextArea();
        messagesArea.setEditable(false);
        messagesArea.setWrapText(true);
        setCenter(messagesArea);

        inputField = new TextField();
        inputField.setPromptText("Napiš zprávu…");

        Button sendButton = new Button("Odeslat");
        sendButton.setOnAction(e -> sendMessage());

        // Enter = odeslat
        inputField.setOnAction(e -> sendMessage());

        HBox bottom = new HBox(8, inputField, sendButton);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(8, 0, 0, 0));
        setBottom(bottom);

        // 1) po přihlášení jednorázově vyzvedneme zprávy, které čekají ve frontě
        pickupOnce();

        // 2) a spustíme polling, aby desktop průběžně tahal nové zprávy z webu
        startQueuePolling(user.getUserId().toString());

        // 3) při zavření okna polling zastavíme
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsW, oldW, newW) -> {
                    if (newW != null) {
                        newW.setOnHidden(e -> stopQueuePolling());
                    }
                });
            }
        });
    }

    private void pickupOnce() {
        new Thread(() -> {
            try {
                List<PayloadMessage> messages = client.pickupMessages(user.getUserId().toString());
                if (messages != null && !messages.isEmpty()) {
                    Platform.runLater(() -> messages.forEach(this::appendMessage));
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Initial pickup error: " + e.getMessage());
            }
        }).start();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        inputField.clear();

        new Thread(() -> {
            try {
                client.sendMessage(user, text);
                // Nevoláme tu pickup hned – polling si to stáhne automaticky.
                // (Kdybychom tu pickup dělali, mohly by se dělat dvojité requesty.)
            } catch (IOException | InterruptedException e) {
                System.out.println("Send error: " + e.getMessage());
            }
        }).start();
    }

    private void appendMessage(PayloadMessage msg) {
        String time = "";
        try {
            var zdt = java.time.Instant.parse(msg.getDate())
                    .atZone(java.time.ZoneId.systemDefault());
            time = " [" + zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "]";
        } catch (Exception ignored) {}


        messagesArea.appendText(
                msg.getSenderName() + time + ": " + msg.getContent() + System.lineSeparator()
        );
    }

    private void startQueuePolling(String userId) {
        stopQueuePolling(); // kdyby už běžel

        polling = true;

        queuePoller = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!polling) return;

            // zabráníme tomu, aby se requesty kupily, když server chvíli odpovídá pomaleji
            if (pollingRequestInFlight) return;
            pollingRequestInFlight = true;

            new Thread(() -> {
                try {
                    List<PayloadMessage> newMessages = client.pickupMessages(userId);

                    if (newMessages != null && !newMessages.isEmpty()) {
                        Platform.runLater(() -> newMessages.forEach(this::appendMessage));
                    }
                } catch (Exception ex) {
                    System.out.println("Polling error: " + ex.getMessage());
                } finally {
                    pollingRequestInFlight = false;
                }
            }).start();
        }));

        queuePoller.setCycleCount(Timeline.INDEFINITE);
        queuePoller.play();
    }

    private void stopQueuePolling() {
        polling = false;
        pollingRequestInFlight = false;

        if (queuePoller != null) {
            queuePoller.stop();
            queuePoller = null;
        }
    }
}
