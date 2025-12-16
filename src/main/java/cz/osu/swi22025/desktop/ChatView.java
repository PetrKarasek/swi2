package cz.osu.swi22025.desktop;

import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.model.json.UserToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main chat room view for the desktop client.
 * Shows the main room and allows sending messages via REST to the same backend.
 */
public class ChatView extends BorderPane {

    private final DesktopClient client;
    private final UserToken user;
    private final TextArea messagesArea;
    private final TextField inputField;

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

        ScrollPane scrollPane = new ScrollPane(messagesArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        setCenter(scrollPane);

        inputField = new TextField();
        inputField.setPromptText("Napiš zprávu…");

        Button sendButton = new Button("Odeslat");
        sendButton.setOnAction(e -> sendMessage());

        HBox bottom = new HBox(5, inputField, sendButton);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(5, 0, 0, 0));
        setBottom(bottom);

        // Po přihlášení ihned vyzvedneme zprávy z fronty (splnění požadavku).
        loadQueuedMessages();
    }

    private void loadQueuedMessages() {
        new Thread(() -> {
            try {
                List<PayloadMessage> messages = client.pickupMessages(user.getUserId().toString());
                if (messages != null && !messages.isEmpty()) {
                    Platform.runLater(() -> {
                        for (PayloadMessage m : messages) {
                            appendMessage(m);
                        }
                    });
                }
            } catch (IOException | InterruptedException e) {
                // for simplicity just log to console
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        inputField.clear();

        new Thread(() -> {
            try {
                client.sendMessage(user, text);
                // po odeslání ihned vyzvedneme zprávy z fronty
                List<PayloadMessage> messages = client.pickupMessages(user.getUserId().toString());
                if (messages != null && !messages.isEmpty()) {
                    Platform.runLater(() -> {
                        for (PayloadMessage m : messages) {
                            appendMessage(m);
                        }
                    });
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void appendMessage(PayloadMessage msg) {
        String time = "";
        try {
            time = " [" + ZonedDateTime.parse(msg.getDate()).format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "]";
        } catch (Exception ignored) {
        }
        messagesArea.appendText(msg.getSenderName() + time + ": " + msg.getContent() + System.lineSeparator());
    }
}


