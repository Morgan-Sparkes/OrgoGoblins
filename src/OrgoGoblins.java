import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class OrgoGoblins extends Application {

    @Override
    public void start(Stage primaryStage) {
        //Load hut
        Image hutImage = new Image("file:hut.png");
        ImageView hutView = new ImageView(hutImage);
        hutView.setFitWidth(100);
        hutView.setFitHeight(100);

        Button hutButton = new Button("", hutView);

        hutButton.setOnAction(e -> {
            CreatureWindow creature = new CreatureWindow();
            creature.show();
        });

        Scene scene = new Scene(hutButton, 120, 120);
        primaryStage.setScene(scene);
        primaryStage.setTitle("OrgoGoblins Hut");
        primaryStage.show();

    }
    public static void main(String[] args) {
        launch(args);
    }
}
