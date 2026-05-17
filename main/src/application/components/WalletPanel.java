package application.components;

import java.util.HashMap;
import java.util.Map;

import application.Market;
import application.Portfolio;
import application.Stock;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.MapChangeListener;
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

public class WalletPanel extends StackPane {

	private static final String FONT = "맑은 고딕";

	private final Market market;
	private final Portfolio portfolio;

	private final Label cashValue;
	private final Label evalValue;
	private final Label totalValue;
	private final Label returnValue;
	private final VBox holdingsBox;
	private final Map<Stock, HBox> holdingRows = new HashMap<>();

	public WalletPanel(double width, double height, Market market, Portfolio portfolio) {
		this.market = market;
		this.portfolio = portfolio;

		getStyleClass().add("panel");
		setPrefSize(width, height);
		setMaxHeight(height);

		Label title = new Label("내 지갑");
		title.setFont(Font.font(FONT, FontWeight.BOLD, 20));
		title.setTextFill(Color.web("#F2F0FF"));

		cashValue = makeValueLabel();
		evalValue = makeValueLabel();
		totalValue = makeValueLabel();
		returnValue = makeValueLabel();

		HBox cashRow = makeStatRow("현금", cashValue);
		HBox evalRow = makeStatRow("평가금액", evalValue);
		HBox totalRow = makeStatRow("총 자산", totalValue);
		HBox retRow = makeStatRow("수익률", returnValue);

		Label hLabel = new Label("보유 종목");
		hLabel.setFont(Font.font(FONT, FontWeight.BOLD, 14));
		hLabel.setTextFill(Color.web("#C8C4FF"));

		holdingsBox = new VBox(4);

		ScrollPane scroll = new ScrollPane(holdingsBox);
		scroll.setFitToWidth(true);
		scroll.setStyle(
			"-fx-background: transparent;" +
			"-fx-background-color: transparent;"
		);
		scroll.setPrefHeight(80);

		VBox layout = new VBox(6, title, cashRow, evalRow, totalRow, retRow, hLabel, scroll);
		layout.setPadding(new Insets(14, 18, 14, 18));
		layout.setAlignment(Pos.TOP_LEFT);
		VBox.setVgrow(scroll, Priority.ALWAYS);

		getChildren().add(layout);

		portfolio.cashProperty().addListener((obs, o, n) -> refreshSummary());

		portfolio.getHoldings().addListener((MapChangeListener<Stock, SimpleIntegerProperty>) change -> {
			rebuildHoldings();
			refreshSummary();
		});

		for (Stock s : market.getStocks()) {
			s.priceProperty().addListener((obs, o, n) -> {
				refreshSummary();
				refreshHoldingRow(s);
			});
		}

		rebuildHoldings();
		refreshSummary();
	}

	private Label makeValueLabel() {
		Label l = new Label();
		l.setFont(Font.font(FONT, FontWeight.BOLD, 15));
		l.setTextFill(Color.WHITE);
		return l;
	}

	private HBox makeStatRow(String name, Label value) {
		Label key = new Label(name);
		key.setFont(Font.font(FONT, FontWeight.NORMAL, 14));
		key.setTextFill(Color.web("#E0E0E0"));

		Region sp = new Region();
		HBox.setHgrow(sp, Priority.ALWAYS);

		HBox row = new HBox(8, key, sp, value);
		row.setAlignment(Pos.CENTER_LEFT);
		row.setPadding(new Insets(2, 0, 2, 0));
		return row;
	}

	private void refreshSummary() {
		cashValue.setText(String.format("%,.0f 원", portfolio.getCash()));
		evalValue.setText(String.format("%,.0f 원", portfolio.getEvaluation()));
		totalValue.setText(String.format("%,.0f 원", portfolio.getTotalAsset()));

		double pct = portfolio.getReturnPercent();
		returnValue.setText(String.format("%+.2f%%", pct));
		if (pct > 0) {
			returnValue.setTextFill(Color.web("#FF6B6B"));
		} else if (pct < 0) {
			returnValue.setTextFill(Color.web("#74B9FF"));
		} else {
			returnValue.setTextFill(Color.WHITE);
		}
	}

	private void rebuildHoldings() {
		holdingsBox.getChildren().clear();
		holdingRows.clear();
		if (portfolio.getHoldings().isEmpty()) {
			Label empty = new Label("보유 종목이 없습니다");
			empty.setFont(Font.font(FONT, FontWeight.NORMAL, 12));
			empty.setTextFill(Color.web("#A8A4D0"));
			holdingsBox.getChildren().add(empty);
			return;
		}
		for (Stock s : portfolio.getHoldings().keySet()) {
			HBox row = buildHoldingRow(s);
			holdingRows.put(s, row);
			holdingsBox.getChildren().add(row);
			refreshHoldingRow(s);
		}
	}

	private HBox buildHoldingRow(Stock s) {
		Label name = new Label(s.getName());
		name.setFont(Font.font(FONT, FontWeight.BOLD, 13));
		name.setTextFill(Color.WHITE);

		Label qty = new Label();
		qty.setFont(Font.font(FONT, FontWeight.NORMAL, 12));
		qty.setTextFill(Color.web("#C8C4FF"));

		VBox left = new VBox(1, name, qty);

		Label pl = new Label();
		pl.setFont(Font.font(FONT, FontWeight.BOLD, 13));

		Region sp = new Region();
		HBox.setHgrow(sp, Priority.ALWAYS);

		HBox row = new HBox(8, left, sp, pl);
		row.setAlignment(Pos.CENTER_LEFT);
		row.setPadding(new Insets(6, 10, 6, 10));
		row.setStyle(
			"-fx-background-color: rgba(255,255,255,0.08);" +
			"-fx-background-radius: 8;"
		);
		row.setUserData(new Label[]{qty, pl});
		row.setOnMouseClicked(e -> market.setSelected(s));
		return row;
	}

	private void refreshHoldingRow(Stock s) {
		HBox row = holdingRows.get(s);
		if (row == null) {
			return;
		}
		Label[] labels = (Label[]) row.getUserData();
		Label qty = labels[0];
		Label pl = labels[1];

		int q = portfolio.getQuantity(s);
		double avg = portfolio.getAvgBuyPrice(s);
		double profit = (s.getPrice() - avg) * q;
		double pct = avg == 0 ? 0 : (s.getPrice() - avg) / avg * 100.0;

		qty.setText(String.format("%,d주  평단 %,.0f", q, avg));
		pl.setText(String.format("%+,.0f (%+.1f%%)", profit, pct));
		if (profit > 0) {
			pl.setTextFill(Color.web("#FF6B6B"));
		} else if (profit < 0) {
			pl.setTextFill(Color.web("#74B9FF"));
		} else {
			pl.setTextFill(Color.WHITE);
		}
	}
}
