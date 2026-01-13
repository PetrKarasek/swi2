package cz.osu.swi22025.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AvatarPickerDialog {

    // povinně stejné názvy jako v backendu (resources/static/avatars)
    public static final List<String> AVATARS = List.of(
            "cat.png", "female.png", "kapuce.png", "male.png", "robot.png"
    );

    /**
     * @param owner parent stage
     * @param baseUrl např. http://localhost:8081
     * @param currentAvatarUrl např. /avatars/cat.png
     * @return vybraný avatarFile (např. cat.png) nebo null když cancel
     */
    public static String show(Stage owner, String baseUrl, String currentAvatarUrl) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Choose Your Avatar");

        Label title = new Label("Choose Your Avatar");
        Label subtitle = new Label("Click on an avatar to select it, then confirm your choice.");

        HBox avatarRow = new HBox(16);
        avatarRow.setAlignment(Pos.CENTER);

        AtomicReference<String> selected = new AtomicReference<>(extractFile(currentAvatarUrl));

        final int size = 80;

        for (String file : AVATARS) {
            String url = baseUrl + "/avatars/" + file;
            ImageView iv = new ImageView(new Image(url, size, size, true, true, true));
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            // jednoduché vizuální zvýraznění výběru:
            iv.setStyle(file.equalsIgnoreCase(selected.get())
                    ? "-fx-effect: dropshadow(gaussian, #2b6cff, 12, 0.6, 0, 0); -fx-background-radius: 999;"
                    : "-fx-opacity: 0.85;");

            iv.setOnMouseClicked(e -> {
                if (e.getButton() != MouseButton.PRIMARY) return;
                selected.set(file);

                // refresh styles
                avatarRow.getChildren().forEach(node -> node.setStyle("-fx-opacity: 0.85;"));
                iv.setStyle("-fx-effect: dropshadow(gaussian, #2b6cff, 12, 0.6, 0, 0);");
            });

            avatarRow.getChildren().add(iv);
        }

        Button cancel = new Button("CANCEL");
        Button confirm = new Button("CONFIRM");

        HBox buttons = new HBox(12, cancel, confirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, title, subtitle, avatarRow, buttons);
        root.setPadding(new Insets(18));
        root.setAlignment(Pos.TOP_CENTER);
        root.setMinWidth(520);

        AtomicReference<String> result = new AtomicReference<>(null);

        cancel.setOnAction(e -> {
            result.set(null);
            dialog.close();
        });

        confirm.setOnAction(e -> {
            result.set(selected.get());
            dialog.close();
        });

        dialog.setScene(new Scene(root));
        dialog.showAndWait();

        return result.get();
    }

    private static String extractFile(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return "cat.png";
        int idx = avatarUrl.lastIndexOf('/');
        return idx >= 0 ? avatarUrl.substring(idx + 1) : avatarUrl;
    }
}
