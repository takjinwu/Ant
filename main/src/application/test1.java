package application;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class test1 extends Application {

    @Override
    public void start(Stage stage) {

        StackPane root = new StackPane();

        // ===== 배경 그라데이션 =====
        Stop[] stops = new Stop[] {
                new Stop(0, Color.web("#06124A")),
                new Stop(0.45, Color.web("#1A0B5E")),
                new Stop(0.75, Color.web("#5B006E")),
                new Stop(1, Color.web("#9B005C"))
        };

        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 0,
                true,
                CycleMethod.NO_CYCLE,
                stops
        );

        root.setStyle(
                "-fx-background-color: linear-gradient(to right, " +
                        "#06124A 0%, " +
                        "#1A0B5E 45%, " +
                        "#5B006E 75%, " +
                        "#9B005C 100%);"
        );

        // ===== 메인 로고 =====
        Label title = new Label("개미증권");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("맑은 고딕", FontWeight.EXTRA_BOLD, 72));

        // ===== 서브 문구 =====
        Label sub = new Label("대한민국 대표 개미 투자 플랫폼");
        sub.setTextFill(Color.WHITE);
        sub.setFont(Font.font("맑은 고딕", FontWeight.BOLD, 24));

        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(title, sub);

        root.getChildren().add(box);

        Scene scene = new Scene(root, 1200, 700);

        stage.setTitle("개미증권");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}