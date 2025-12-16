package cz.osu.swi22025.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Simple JavaFX desktop client that connects to the same backend and database as the web client.
 */
public class DesktopApp extends Application {

    private final DesktopClient client = new DesktopClient();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SWI2 Chat Desktop");

        LoginView loginView = new LoginView(client, userToken -> {
            // After successful login, show main chat view (main room)
            ChatView chatView = new ChatView(client, userToken);
            primaryStage.setScene(new Scene(chatView, 800, 600));
        });

        primaryStage.setScene(new Scene(loginView, 500, 350));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


