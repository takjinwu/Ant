package application;

import application.components.ChartPanel;
import application.components.NewsPanel;
import application.components.OrderPanel;
import application.components.PanelCard;
import application.components.StockListPanel;
import application.components.WalletPanel;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
		PanelCard brand = new PanelCard("개미증권", 26, FontWeight.EXTRA_BOLD, 280, 56) {};

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
		HBox center = new HBox(14, buildLeftColumn(), new StockListPanel(220, COLUMN_H), new NewsPanel(380, COLUMN_H));
		center.setAlignment(Pos.TOP_LEFT);
		return center;
	}

	private VBox buildLeftColumn() {
		ChartPanel chart = new ChartPanel(440, CHART_H);

		OrderPanel order = new OrderPanel(214, BOTTOM_H);
		WalletPanel wallet = new WalletPanel(214, BOTTOM_H);
		HBox bottom = new HBox(GAP, order, wallet);

		VBox left = new VBox(GAP, chart, bottom);
		left.setPrefHeight(COLUMN_H);
		return left;
	}

	public static void main(String[] args) {
		launch(args);
	}
}
