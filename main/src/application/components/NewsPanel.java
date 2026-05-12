package application.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 뉴스 패널 + 우상단 "현재 () 턴 / 00:00" 칩을 함께 묶어서 제공.
 * NewsPanel 자체는 Pane이라 자식(뉴스 카드 + 턴 칩)을 절대좌표로 배치한다.
 */
public class NewsPanel extends Pane {

	private static final String FONT = "맑은 고딕";

	private final PanelCard news;
	private final Label turnPill;

	public NewsPanel(double width, double height) {
		this.news = new PanelCard("뉴스", 28, FontWeight.BOLD, width, height) {};

		this.turnPill = new Label("현재 () 턴 / 00:00");
		turnPill.setFont(Font.font(FONT, FontWeight.BOLD, 13));
		turnPill.setTextFill(Color.WHITE);
		turnPill.getStyleClass().add("turn-pill");
		turnPill.setPrefSize(170, 30);
		turnPill.setAlignment(Pos.CENTER);

		getChildren().addAll(news, turnPill);
		setPrefSize(width, height);
		setMaxHeight(height);

		news.setLayoutX(0);
		news.setLayoutY(0);
		turnPill.setLayoutX(width - 180);
		turnPill.setLayoutY(10);
	}

	public void setTurnText(String text) {
		turnPill.setText(text);
	}
}
