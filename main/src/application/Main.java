package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class Main extends Application {

	private static final String FONT = "맑은 고딕";

	private static final double CHART_H = 360;
	private static final double BOTTOM_H = 180;
	private static final double GAP = 12;
	private static final double COLUMN_H = CHART_H + GAP + BOTTOM_H;

	@Override
	public void start(Stage primaryStage) {
		try {
			BorderPane root = new BorderPane();
			root.setPadding(new Insets(18));
			root.setStyle(
				"-fx-background-color: linear-gradient(to right, " +
					"#06124A 0%, " +
					"#1A0B5E 45%, " +
					"#5B006E 75%, " +
					"#9B005C 100%);"
			);

			root.setTop(buildTop());
			root.setCenter(buildCenter());

			Scene scene = new Scene(root, 1200, 700);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setTitle("개미증권");
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private HBox buildTop() {
		Region brand = panel("개미증권", 26, FontWeight.EXTRA_BOLD);
		brand.setPrefSize(280, 56);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		Button fastForward = new Button("▶▶");
		fastForward.setFont(Font.font(FONT, FontWeight.EXTRA_BOLD, 22));
		fastForward.setPrefSize(80, 56);
		fastForward.getStyleClass().add("ghost-button");

		HBox top = new HBox(12, brand, spacer, fastForward);
		top.setPadding(new Insets(0, 0, 14, 0));
		top.setAlignment(Pos.CENTER_LEFT);
		return top;
	}

	private HBox buildCenter() {
		HBox center = new HBox(14, buildLeftColumn(), buildStockList(), buildNewsColumn());
		center.setAlignment(Pos.TOP_LEFT);
		return center;
	}

	private VBox buildLeftColumn() {
		Region chart = panel("차트", 28, FontWeight.BOLD);
		chart.setPrefSize(440, CHART_H);

		Region order = panel("주문창", 22, FontWeight.BOLD);
		order.setPrefSize(214, BOTTOM_H);

		Region wallet = panel("보유자산 표기\n지갑", 20, FontWeight.BOLD);
		wallet.setPrefSize(214, BOTTOM_H);

		HBox bottom = new HBox(GAP, order, wallet);

		VBox left = new VBox(GAP, chart, bottom);
		left.setPrefHeight(COLUMN_H);
		return left;
	}

	private Region buildStockList() {
		Region stocks = panel("주식\n목록", 24, FontWeight.BOLD);
		stocks.setPrefSize(220, COLUMN_H);
		stocks.setMaxHeight(COLUMN_H);
		return stocks;
	}

	private Pane buildNewsColumn() {
		double newsW = 380;

		Region news = panel("뉴스", 28, FontWeight.BOLD);
		news.setPrefSize(newsW, COLUMN_H);
		news.setMaxHeight(COLUMN_H);

		Label turn = new Label("현재 () 턴 / 00:00");
		turn.setFont(Font.font(FONT, FontWeight.BOLD, 13));
		turn.setTextFill(Color.WHITE);
		turn.getStyleClass().add("turn-pill");
		turn.setPrefSize(170, 30);
		turn.setAlignment(Pos.CENTER);

		Pane wrapper = new Pane(news, turn);
		wrapper.setPrefSize(newsW, COLUMN_H);
		wrapper.setMaxHeight(COLUMN_H);
		news.layoutXProperty().set(0);
		news.layoutYProperty().set(0);
		turn.layoutXProperty().set(newsW - 180);
		turn.layoutYProperty().set(10);
		return wrapper;
	}

	private Region panel(String text, double fontSize, FontWeight weight) {
		Label label = new Label(text);
		label.setFont(Font.font(FONT, weight, fontSize));
		label.setTextFill(Color.web("#F2F0FF"));
		label.setAlignment(Pos.CENTER);
		label.setTextAlignment(TextAlignment.CENTER);

		StackPane card = new StackPane(label);
		card.getStyleClass().add("panel");
		return card;
	}

	public static void main(String[] args) {
		launch(args);
	}
}
