package cz.osu.swi22025.desktop;

import cz.osu.swi22025.model.json.UserToken;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Simple JavaFX desktop client that connects to the same backend and database as the web client.
 */
public class DesktopApp extends Application {

    private final DesktopClient client = new DesktopClient();
    private Scene scene;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("SWI2 Chat Desktop");

        // jedna Scene napořád, jen měníme root
        LoginView loginView = createLoginView();
        scene = new Scene(loginView, 900, 650);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private LoginView createLoginView() {
        return new LoginView(client, userToken -> {

            // ✅ po loginu odešli pending zprávy z host módu
            PendingStore store = new PendingStore();
            var pending = store.load();
            for (var p : pending) {
                try {
                    client.sendMessage(userToken, p.getContent());
                } catch (Exception ignored) {}
            }
            store.clear();
            // ✅ konec pending

            showChat(userToken);
        });
    }

    private void showChat(UserToken userToken) {
        ChatView chatView = new ChatView(client, userToken, () -> {
            // logout → zpět na login
            scene.setRoot(createLoginView());
            // (volitelně) přenastav velikost okna
            primaryStage.setWidth(900);
            primaryStage.setHeight(650);
        });

        scene.setRoot(chatView);
        primaryStage.setWidth(1100);
        primaryStage.setHeight(750);
    }

    public static void main(String[] args) {
        launch(args);
    }
}


