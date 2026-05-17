package application.components;

import application.Market;
import application.Portfolio;
import application.Stock;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class OrderPanel extends StackPane {

	private static final String FONT = "맑은 고딕";

	private final Market market;
	private final Portfolio portfolio;

	private final Label stockNameLabel;
	private final Label priceLabel;
	private final TextField qtyField;
	private final Label totalLabel;
	private final Label messageLabel;
	private final Label holdingLabel;

	public OrderPanel(double width, double height, Market market, Portfolio portfolio) {
		this.market = market;
		this.portfolio = portfolio;

		getStyleClass().add("panel");
		setPrefSize(width, height);
		setMaxHeight(height);

		Label title = new Label("주문창");
		title.setFont(Font.font(FONT, FontWeight.BOLD, 20));
		title.setTextFill(Color.web("#F2F0FF"));

		stockNameLabel = new Label();
		stockNameLabel.setFont(Font.font(FONT, FontWeight.EXTRA_BOLD, 18));
		stockNameLabel.setTextFill(Color.WHITE);

		priceLabel = new Label();
		priceLabel.setFont(Font.font(FONT, FontWeight.BOLD, 16));
		priceLabel.setTextFill(Color.web("#FFD166"));

		holdingLabel = new Label();
		holdingLabel.setFont(Font.font(FONT, FontWeight.NORMAL, 13));
		holdingLabel.setTextFill(Color.web("#E0E0E0"));

		Label qtyLbl = new Label("수량");
		qtyLbl.setFont(Font.font(FONT, FontWeight.BOLD, 14));
		qtyLbl.setTextFill(Color.WHITE);

		qtyField = new TextField("1");
		qtyField.setPrefWidth(110);
		qtyField.setStyle(
			"-fx-background-color: rgba(255,255,255,0.85);" +
			"-fx-text-fill: #1A0B5E;" +
			"-fx-font-size: 14px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 8;"
		);
		qtyField.setTextFormatter(new TextFormatter<>(c ->
			c.getControlNewText().matches("\\d{0,7}") ? c : null
		));
		qtyField.textProperty().addListener((obs, o, n) -> updateTotal());

		HBox qtyRow = new HBox(10, qtyLbl, qtyField);
		qtyRow.setAlignment(Pos.CENTER_LEFT);

		totalLabel = new Label();
		totalLabel.setFont(Font.font(FONT, FontWeight.BOLD, 14));
		totalLabel.setTextFill(Color.web("#E0E0E0"));

		Button buyBtn = new Button("매수");
		buyBtn.setStyle(buttonStyle("#FF416C", "#FF4B2B"));
		buyBtn.setPrefWidth(110);
		buyBtn.setOnAction(e -> doBuy());

		Button sellBtn = new Button("매도");
		sellBtn.setStyle(buttonStyle("#2193B0", "#6DD5ED"));
		sellBtn.setPrefWidth(110);
		sellBtn.setOnAction(e -> doSell());

		HBox btnRow = new HBox(10, buyBtn, sellBtn);
		btnRow.setAlignment(Pos.CENTER);

		messageLabel = new Label("");
		messageLabel.setFont(Font.font(FONT, FontWeight.BOLD, 12));
		messageLabel.setTextFill(Color.web("#FFD166"));

		Region grow = new Region();
		VBox.setVgrow(grow, Priority.ALWAYS);

		VBox layout = new VBox(8,
			title,
			stockNameLabel,
			priceLabel,
			holdingLabel,
			qtyRow,
			totalLabel,
			btnRow,
			messageLabel
		);
		layout.setPadding(new Insets(14, 18, 14, 18));
		layout.setAlignment(Pos.TOP_LEFT);

		getChildren().add(layout);

		market.selectedProperty().addListener((obs, o, n) -> refresh());

		for (Stock s : market.getStocks()) {
			s.priceProperty().addListener((obs, o, n) -> {
				if (market.getSelected() == s) {
					refresh();
				}
			});
		}

		refresh();
	}

	private String buttonStyle(String c1, String c2) {
		return "-fx-background-color: linear-gradient(to right, " + c1 + ", " + c2 + ");" +
			"-fx-text-fill: white;" +
			"-fx-font-size: 15px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 12;" +
			"-fx-padding: 8 18 8 18;" +
			"-fx-cursor: hand;" +
			"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 8, 0.25, 0, 2);";
	}

	private void refresh() {
		Stock s = market.getSelected();
		if (s == null) {
			return;
		}
		stockNameLabel.setText(s.getName());
		priceLabel.setText(String.format("현재가  %,.0f 원", s.getPrice()));
		holdingLabel.setText(String.format("보유 %,d주  ·  평단 %,.0f원",
			portfolio.getQuantity(s), portfolio.getAvgBuyPrice(s)));
		updateTotal();
	}

	private void updateTotal() {
		Stock s = market.getSelected();
		if (s == null) {
			totalLabel.setText("");
			return;
		}
		int qty = parseQty();
		double total = s.getPrice() * qty;
		totalLabel.setText(String.format("예상 금액  %,.0f 원", total));
	}

	private int parseQty() {
		try {
			String t = qtyField.getText();
			if (t == null || t.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void doBuy() {
		Stock s = market.getSelected();
		if (s == null) {
			return;
		}
		int qty = parseQty();
		if (qty <= 0) {
			showMessage("수량을 입력하세요.", "#FFB86B");
			return;
		}
		boolean ok = portfolio.buy(s, qty);
		if (ok) {
			showMessage(String.format("%s %,d주 매수 완료", s.getName(), qty), "#7BFF8A");
		} else {
			showMessage("현금이 부족합니다.", "#FF6B6B");
		}
		refresh();
	}

	private void doSell() {
		Stock s = market.getSelected();
		if (s == null) {
			return;
		}
		int qty = parseQty();
		if (qty <= 0) {
			showMessage("수량을 입력하세요.", "#FFB86B");
			return;
		}
		boolean ok = portfolio.sell(s, qty);
		if (ok) {
			showMessage(String.format("%s %,d주 매도 완료", s.getName(), qty), "#7BFF8A");
		} else {
			showMessage("보유 수량이 부족합니다.", "#FF6B6B");
		}
		refresh();
	}

	private void showMessage(String text, String colorHex) {
		messageLabel.setText(text);
		messageLabel.setTextFill(Color.web(colorHex));
	}
}
