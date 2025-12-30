package cz.osu.swi22025.desktop;

import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.model.json.UserToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Login + Register on the left
 * Guest "main room preview" + pending queue input on the right
 */
public class LoginView extends BorderPane {

    public interface LoginSuccessHandler {
        void onSuccess(UserToken token);
    }

    private final DesktopClient client;
    private final LoginSuccessHandler successHandler;

    // LEFT: login/register
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label statusLabel = new Label();

    // RIGHT: guest pending queue
    private final ListView<String> guestPreview = new ListView<>();
    private final TextField guestInput = new TextField();
    private final Button guestSendButton = new Button("SEND");

    private final PendingStore pendingStore = new PendingStore();
    private final List<PayloadMessage> pending = new ArrayList<>();

    public LoginView(DesktopClient client, LoginSuccessHandler successHandler) {
        this.client = client;
        this.successHandler = successHandler;

        setPadding(new Insets(16));

        // layout: left + right
        HBox root = new HBox(16);
        root.setFillHeight(true);

        VBox left = buildLeftLoginPanel();
        VBox right = buildRightGuestPanel();

        HBox.setHgrow(left, Priority.NEVER);
        HBox.setHgrow(right, Priority.ALWAYS);

        left.setPrefWidth(360);
        right.setPrefWidth(520);

        root.getChildren().addAll(left, right);
        setCenter(root);

        // load pending from disk to preview
        loadPendingToPreview();
    }

    private VBox buildLeftLoginPanel() {
        Label title = new Label("SWI2 Chat – Přihlášení");
        title.setFont(Font.font(18));

        Label subtitle = new Label("Přihlas se pomocí účtu uloženého ve společné databázi.");
        subtitle.setWrapText(true);

        usernameField.setPromptText("Uživatelské jméno");
        passwordField.setPromptText("Heslo");

        Button loginBtn = new Button("Přihlásit se");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Button registerBtn = new Button("Zaregistrovat se");
        registerBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> doLogin());
        registerBtn.setOnAction(e -> doRegister());

        statusLabel.setWrapText(true);

        VBox box = new VBox(10,
                title,
                subtitle,
                new Separator(),
                usernameField,
                passwordField,
                loginBtn,
                registerBtn,
                statusLabel
        );
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-radius: 8; -fx-background-radius: 8;");
        return box;
    }

    private VBox buildRightGuestPanel() {
        // Header like web
        Label header = new Label("Hlavní místnost · Nepřihlášený uživatel");
        header.setFont(Font.font(14));

        Label hint = new Label("Z důvodu zadání mohou zprávy číst pouze přihlášení uživatelé.\n" +
                "Níže může host napsat zprávu – uloží se do fronty a odešle po přihlášení.");
        hint.setWrapText(true);

        // Preview: show only placeholder info + pending list (not real chat)
        guestPreview.setFocusTraversable(false);
        guestPreview.setPrefHeight(320);

        VBox previewBox = new VBox(8, hint, guestPreview);
        previewBox.setPadding(new Insets(12));
        previewBox.setStyle("-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-radius: 8; -fx-background-radius: 8;");

        guestInput.setPromptText("Napiš zprávu (bude ve frontě do přihlášení)");
        guestSendButton.setDisable(true);

        guestInput.textProperty().addListener((obs, oldVal, newVal) -> {
            guestSendButton.setDisable(newVal == null || newVal.trim().isEmpty());
        });

        guestSendButton.setOnAction(e -> queueGuestMessage());
        guestInput.setOnAction(e -> queueGuestMessage());

        HBox inputRow = new HBox(8, guestInput, guestSendButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(guestInput, Priority.ALWAYS);

        VBox right = new VBox(10, header, previewBox, inputRow);
        right.setPadding(new Insets(16));
        right.setStyle("-fx-background-color: #f6f7f9; -fx-border-color: #dcdcdc; -fx-border-radius: 8; -fx-background-radius: 8;");
        VBox.setVgrow(previewBox, Priority.ALWAYS);

        return right;
    }

    private void loadPendingToPreview() {
        pending.clear();
        pending.addAll(pendingStore.load());

        guestPreview.getItems().clear();

        // Static “demo” messages like on web (just to show layout)
        guestPreview.getItems().add("alice: Vítej v hlavní místnosti 🎉");
        guestPreview.getItems().add("bob: Přihlas se, aby ses připojil do konverzace.");

        if (!pending.isEmpty()) {
            guestPreview.getItems().add("— Pending (odešle se po loginu) —");
            for (PayloadMessage p : pending) {
                guestPreview.getItems().add("GUEST (queued): " + p.getContent());
            }
        }
    }

    private void queueGuestMessage() {
        String text = guestInput.getText() == null ? "" : guestInput.getText().trim();
        if (text.isEmpty()) return;

        PayloadMessage pm = new PayloadMessage();
        pm.setSenderName("GUEST");
        pm.setReceiverChatRoomId("1");
        pm.setContent(text);
        pm.setDate(Instant.now().toString());

        pending.add(pm);
        pendingStore.save(pending);

        guestInput.clear();
        loadPendingToPreview();
    }

    private void doLogin() {
        statusLabel.setText("");

        String u = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String p = passwordField.getText() == null ? "" : passwordField.getText().trim();
        if (u.isEmpty() || p.isEmpty()) {
            statusLabel.setText("Vyplň uživatelské jméno a heslo.");
            return;
        }

        new Thread(() -> {
            try {
                UserToken token = client.login(u, p);

                Platform.runLater(() -> {
                    statusLabel.setText("Přihlášení OK.");
                    successHandler.onSuccess(token); // DesktopApp už pending po loginu odešle (jak jsme přidali)
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Přihlášení selhalo: " + ex.getMessage()));
            }
        }).start();
    }

    private void doRegister() {
        statusLabel.setText("");

        String u = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String p = passwordField.getText() == null ? "" : passwordField.getText().trim();
        if (u.isEmpty() || p.isEmpty()) {
            statusLabel.setText("Vyplň uživatelské jméno a heslo.");
            return;
        }

        new Thread(() -> {
            try {
                UserToken token = client.register(u, p);

                Platform.runLater(() -> {
                    statusLabel.setText("Registrace OK.");
                    successHandler.onSuccess(token);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Registrace selhala: " + ex.getMessage()));
            }
        }).start();
    }
}
