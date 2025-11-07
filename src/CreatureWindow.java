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

        startMoving();

    }

    private void startMoving() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(500), e -> moveCreature())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void moveCreature() {
        double x = random.nextDouble() * (getWidth() - creatureView.getFitWidth());
        double y = random.nextDouble() * (getHeight() - creatureView.getFitHeight());
        creatureView.setLayoutX(x);
        creatureView.setLayoutY(y);
    }
}
