package cz.osu.swi22025.desktop;

import cz.osu.swi22025.model.json.UserToken;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * JavaFX login / signup screen for the desktop client.
 */
public class LoginView extends VBox {

    public interface LoginSuccessHandler {
        void onSuccess(UserToken userToken);
    }

    private final DesktopClient client;
    private final LoginSuccessHandler successHandler;

    public LoginView(DesktopClient client, LoginSuccessHandler successHandler) {
        this.client = client;
        this.successHandler = successHandler;

        setSpacing(10);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER);

        Label title = new Label("SWI2 Chat – Přihlášení");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Uživatelské jméno");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Heslo");

        Label statusLabel = new Label();

        Button loginButton = new Button("Přihlásit se");
        Button signupButton = new Button("Zaregistrovat se");

        loginButton.setOnAction(e -> {
            statusLabel.setText("");
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Vyplň uživatelské jméno i heslo.");
                return;
            }
            new Thread(() -> {
                try {
                    UserToken token = client.login(username, password);
                    javafx.application.Platform.runLater(() -> successHandler.onSuccess(token));
                } catch (IOException | InterruptedException ex) {
                    javafx.application.Platform.runLater(() -> statusLabel.setText("Přihlášení selhalo: " + ex.getMessage()));
                }
            }).start();
        });

        signupButton.setOnAction(e -> {
            statusLabel.setText("");
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Vyplň uživatelské jméno i heslo.");
                return;
            }
            new Thread(() -> {
                try {
                    client.signup(username, password);
                    UserToken token = client.login(username, password);
                    javafx.application.Platform.runLater(() -> successHandler.onSuccess(token));
                } catch (IOException | InterruptedException ex) {
                    javafx.application.Platform.runLater(() -> statusLabel.setText("Registrace selhala: " + ex.getMessage()));
                }
            }).start();
        });

        getChildren().addAll(title, usernameField, passwordField, loginButton, signupButton, statusLabel);
    }
}


