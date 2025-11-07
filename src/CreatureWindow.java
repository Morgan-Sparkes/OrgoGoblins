import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Random;

public class CreatureWindow extends Stage {

    private ImageView creatureView;
    private Random random = new Random();

    public CreatureWindow() {
        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(true);

        Image creatureImage = new Image("file:creature.png");
        creatureView = new ImageView(creatureImage);
        creatureView.setFitWidth(80);
        creatureView.setFitHeight(80);

        Pane root = new Pane(creatureView);

        Scene scene = new Scene(root, 200, 200);
        scene.setFill(null);

        setScene(scene);

        startMovingDesktop();

    }

    private void startMovingDesktop() {
        Timeline timeline = new Timeline(new  KeyFrame(Duration.millis(500), e -> moveAcrossScreen()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

    }

    private void moveAcrossScreen() {
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();

        double x = random.nextDouble() * (screenWidth - getWidth());
        double y = random.nextDouble() * (screenHeight - getHeight());
        setX(x);
        setY(y);

    }
}
