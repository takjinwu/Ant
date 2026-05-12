package application.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** 공용 글래스 카드 베이스 — 모든 패널 컴포넌트가 상속해서 사용. */
public class PanelCard extends StackPane {

	protected static final String FONT = "맑은 고딕";

	protected final Label titleLabel;

	public PanelCard(String title, double fontSize, FontWeight weight, double width, double height) {
		this.titleLabel = new Label(title);
		titleLabel.setFont(Font.font(FONT, weight, fontSize));
		titleLabel.setTextFill(Color.web("#F2F0FF"));
		titleLabel.setAlignment(Pos.CENTER);
		titleLabel.setTextAlignment(TextAlignment.CENTER);

		getChildren().add(titleLabel);
		getStyleClass().add("panel");

		setPrefSize(width, height);
		setMaxHeight(height);
	}
}
