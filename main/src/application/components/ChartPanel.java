package application.components;

import java.util.List;

import application.Market;
import application.Stock;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ChartPanel extends StackPane {

	private static final String FONT = "맑은 고딕";

	private final Market market;
	private final Label nameLabel;
	private final Label priceLabel;
	private final Label changeLabel;
	private final LineChart<Number, Number> chart;
	private final NumberAxis xAxis;
	private final NumberAxis yAxis;
	private final XYChart.Series<Number, Number> series;

	public ChartPanel(double width, double height, Market market) {
		this.market = market;

		getStyleClass().add("panel");
		setPrefSize(width, height);
		setMaxHeight(height);

		nameLabel = new Label();
		nameLabel.setFont(Font.font(FONT, FontWeight.EXTRA_BOLD, 28));
		nameLabel.setTextFill(Color.WHITE);

		priceLabel = new Label();
		priceLabel.setFont(Font.font(FONT, FontWeight.BOLD, 26));
		priceLabel.setTextFill(Color.WHITE);

		changeLabel = new Label();
		changeLabel.setFont(Font.font(FONT, FontWeight.BOLD, 18));

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		HBox header = new HBox(14, nameLabel, spacer, priceLabel, changeLabel);
		header.setAlignment(Pos.CENTER_LEFT);
		header.setPadding(new Insets(0, 4, 0, 4));

		xAxis = new NumberAxis();
		xAxis.setLabel("턴");
		xAxis.setForceZeroInRange(false);
		xAxis.setTickLabelFill(Color.web("#E8E8E8"));

		yAxis = new NumberAxis();
		yAxis.setLabel("가격");
		yAxis.setForceZeroInRange(false);
		yAxis.setTickLabelFill(Color.web("#E8E8E8"));

		chart = new LineChart<>(xAxis, yAxis);
		chart.setLegendVisible(false);
		chart.setAnimated(false);
		chart.setCreateSymbols(true);
		chart.setStyle(
			"-fx-background-color: transparent;" +
			"CHART_COLOR_1: #FFD166;"
		);

		series = new XYChart.Series<>();
		chart.getData().add(series);

		VBox layout = new VBox(10, header, chart);
		layout.setPadding(new Insets(18, 22, 18, 22));
		VBox.setVgrow(chart, Priority.ALWAYS);

		getChildren().add(layout);

		market.selectedProperty().addListener((obs, oldV, newV) -> refresh());

		market.turnCounterProperty().addListener((obs, oldV, newV) -> refresh());

		refresh();
	}

	private void refresh() {
		Stock s = market.getSelected();
		if (s == null) {
			return;
		}

		nameLabel.setText(s.getName() + "  " + s.getCode());
		priceLabel.setText(String.format("%,.0f 원", s.getPrice()));

		double pct = s.getChangePercent();
		changeLabel.setText(String.format("%+.2f%%", pct));
		if (pct > 0) {
			changeLabel.setTextFill(Color.web("#FF6B6B"));
		} else if (pct < 0) {
			changeLabel.setTextFill(Color.web("#74B9FF"));
		} else {
			changeLabel.setTextFill(Color.web("#E0E0E0"));
		}

		series.getData().clear();
		List<Double> hist = s.getHistory();
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (int i = 0; i < hist.size(); i++) {
			double v = hist.get(i);
			series.getData().add(new XYChart.Data<>(i, v));
			if (v < min) min = v;
			if (v > max) max = v;
		}

		if (min != Double.MAX_VALUE) {
			double pad = Math.max((max - min) * 0.15, max * 0.02);
			yAxis.setAutoRanging(false);
			yAxis.setLowerBound(Math.max(0, min - pad));
			yAxis.setUpperBound(max + pad);
			yAxis.setTickUnit(Math.max(1, (max + pad - (min - pad)) / 6));
		}
	}
}
