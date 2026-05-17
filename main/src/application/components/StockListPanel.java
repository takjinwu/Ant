package application.components;

import java.util.HashMap;
import java.util.Map;

import application.Market;
import application.Stock;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class StockListPanel extends StackPane {

	private static final String FONT = "맑은 고딕";

	private final Market market;
	private final Map<Stock, HBox> rowMap = new HashMap<>();

	public StockListPanel(double width, double height, Market market) {
		this.market = market;

		getStyleClass().add("panel");
		setPrefSize(width, height);
		setMaxHeight(height);

		Label title = new Label("주식 목록");
		title.setFont(Font.font(FONT, FontWeight.BOLD, 24));
		title.setTextFill(Color.web("#F2F0FF"));

		VBox listBox = new VBox(8);
		listBox.setPadding(new Insets(8, 16, 16, 16));

		for (Stock s : market.getStocks()) {
			HBox row = buildRow(s);
			rowMap.put(s, row);
			listBox.getChildren().add(row);

			row.setOnMouseClicked(e -> market.setSelected(s));

			s.priceProperty().addListener((obs, oldV, newV) -> refreshRow(s));
		}

		ScrollPane scroll = new ScrollPane(listBox);
		scroll.setFitToWidth(true);
		scroll.setStyle(
			"-fx-background: transparent;" +
			"-fx-background-color: transparent;"
		);

		VBox layout = new VBox(6, title, scroll);
		layout.setPadding(new Insets(16, 10, 14, 10));
		layout.setAlignment(Pos.TOP_CENTER);
		VBox.setVgrow(scroll, Priority.ALWAYS);

		getChildren().add(layout);

		market.selectedProperty().addListener((obs, oldV, newV) -> {
			if (oldV != null) {
				styleRow(rowMap.get(oldV), false);
			}
			if (newV != null) {
				styleRow(rowMap.get(newV), true);
			}
		});

		if (market.getSelected() != null) {
			styleRow(rowMap.get(market.getSelected()), true);
		}

		for (Stock s : market.getStocks()) {
			refreshRow(s);
		}
	}

	private HBox buildRow(Stock s) {
		Label name = new Label(s.getName());
		name.setFont(Font.font(FONT, FontWeight.BOLD, 16));
		name.setTextFill(Color.WHITE);

		Label code = new Label(s.getCode());
		code.setFont(Font.font(FONT, FontWeight.NORMAL, 11));
		code.setTextFill(Color.web("#C8C4FF"));

		VBox left = new VBox(2, name, code);

		Label price = new Label();
		price.setFont(Font.font(FONT, FontWeight.BOLD, 16));
		price.setTextFill(Color.WHITE);

		Label change = new Label();
		change.setFont(Font.font(FONT, FontWeight.BOLD, 13));

		VBox right = new VBox(2, price, change);
		right.setAlignment(Pos.CENTER_RIGHT);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		HBox row = new HBox(10, left, spacer, right);
		row.setAlignment(Pos.CENTER_LEFT);
		row.setPadding(new Insets(10, 14, 10, 14));
		row.setUserData(new Label[] {price, change});
		styleRow(row, false);
		row.setOnMouseEntered(e -> {
			if (market.getSelected() != s) {
				row.setStyle(rowBaseStyle(true, false));
			}
		});
		row.setOnMouseExited(e -> {
			if (market.getSelected() != s) {
				row.setStyle(rowBaseStyle(false, false));
			}
		});

		return row;
	}

	private void refreshRow(Stock s) {
		HBox row = rowMap.get(s);
		if (row == null) {
			return;
		}
		Label[] labels = (Label[]) row.getUserData();
		Label price = labels[0];
		Label change = labels[1];
		price.setText(String.format("%,.0f", s.getPrice()));

		double pct = s.getChangePercent();
		change.setText(String.format("%+.2f%%", pct));
		if (pct > 0) {
			change.setTextFill(Color.web("#FF6B6B"));
		} else if (pct < 0) {
			change.setTextFill(Color.web("#74B9FF"));
		} else {
			change.setTextFill(Color.web("#E0E0E0"));
		}
	}

	private void styleRow(HBox row, boolean selected) {
		if (row == null) {
			return;
		}
		row.setStyle(rowBaseStyle(false, selected));
	}

	private String rowBaseStyle(boolean hover, boolean selected) {
		if (selected) {
			return "-fx-background-color: rgba(255,255,255,0.28);" +
				"-fx-background-radius: 12;" +
				"-fx-border-color: rgba(255,255,255,0.85);" +
				"-fx-border-width: 1.4;" +
				"-fx-border-radius: 12;" +
				"-fx-cursor: hand;";
		}
		if (hover) {
			return "-fx-background-color: rgba(255,255,255,0.16);" +
				"-fx-background-radius: 12;" +
				"-fx-border-color: rgba(255,255,255,0.55);" +
				"-fx-border-width: 1;" +
				"-fx-border-radius: 12;" +
				"-fx-cursor: hand;";
		}
		return "-fx-background-color: rgba(255,255,255,0.08);" +
			"-fx-background-radius: 12;" +
			"-fx-border-color: rgba(255,255,255,0.30);" +
			"-fx-border-width: 1;" +
			"-fx-border-radius: 12;" +
			"-fx-cursor: hand;";
	}
}
