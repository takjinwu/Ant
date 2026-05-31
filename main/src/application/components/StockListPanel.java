package application.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.function.BiConsumer;

/**
 * StockListPanel
 * ─ 거래 가능한 종목 목록을 표시하는 패널
 * ─ 종목 행을 클릭하면 선택 리스너(setOnStockSelected)로 종목명/현재가를 전달
 * → OrderPanel.setSelectedStock(name, price) 에 연결해서 사용
 * ─ 종목명 앞 상태 점: 상승(빨강), 하락(파랑)
 */
public class StockListPanel extends VBox {

    private final Map<String, Long> stocks = new LinkedHashMap<>();
    private final Map<String, Long> prevPrices = new LinkedHashMap<>();  // 이전 가격 (등락률 계산용)
    private BiConsumer<String, Long> onStockSelected = null;

    private final VBox rowBox;
    private HBox selectedRow = null;

    // 실제 차트 데이터 기반 색상 판단을 위해 ChartPanel 참조 보관
    private ChartPanel chartPanel = null;

    public StockListPanel(double width, double height) {
        setPrefSize(width, height);
        setMaxSize(width, height);
        setMinSize(width, height);

        setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 22;" +
            "-fx-border-width: 1.2;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 18, 0.2, 0, 4);"
        );
        setPadding(new Insets(20, 18, 18, 18));
        setSpacing(14);

        Label header = new Label("📈 주식 목록");
        header.setFont(Font.font("SUIT", FontWeight.EXTRA_BOLD, 17));
        header.setTextFill(Color.web("#FFFFFF"));

        Label nameCol = new Label("종목명");
        nameCol.setFont(Font.font("SUIT", FontWeight.BOLD, 12));
        nameCol.setTextFill(Color.web("#AAB4D4"));

        Region headSpacer = new Region();
        HBox.setHgrow(headSpacer, Priority.ALWAYS);

        Label priceCol = new Label("현재가");
        priceCol.setFont(Font.font("SUIT", FontWeight.BOLD, 12));
        priceCol.setTextFill(Color.web("#AAB4D4"));

        HBox colHeader = new HBox(8, nameCol, headSpacer, priceCol);
        colHeader.setAlignment(Pos.CENTER_LEFT);
        colHeader.setPadding(new Insets(0, 12, 0, 12));

        registerDefaultStocks();

        rowBox = new VBox(6);
        for (Map.Entry<String, Long> e : stocks.entrySet()) {
            rowBox.getChildren().add(buildStockRow(e.getKey(), e.getValue()));
        }

        ScrollPane scroll = new ScrollPane(rowBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;"
        );
        scroll.setPadding(new Insets(0));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, colHeader, scroll);
    }

    private void registerDefaultStocks() {
        stocks.put("삼성전자", 75_000L);
        stocks.put("SK하이닉스", 180_000L);
        stocks.put("셀트리온", 175_000L);
        stocks.put("유한양행", 120_000L);
        stocks.put("한화에어로스페이스", 320_000L);
        stocks.put("LIG넥스원", 240_000L);
        stocks.put("LG에너지솔루션", 380_000L);
        stocks.put("에코프로비엠", 180_000L);
        stocks.put("네이버", 210_000L);
        stocks.put("카카오", 42_000L);
        stocks.put("현대차", 250_000L);
        stocks.put("기아", 105_000L);
    }

    /** ChartPanel 참조를 설정합니다. 설정 후 동그라미 색상이 실제 차트 데이터와 동기화됩니다. */
    public void setChartPanel(ChartPanel cp) {
        this.chartPanel = cp;
    }

    public void setOnStockSelected(BiConsumer<String, Long> listener) {
        this.onStockSelected = listener;
    }

    public long getPrice(String stockName) {
        return stocks.getOrDefault(stockName, 0L);
    }


    /** 전체 종목 맵을 반환합니다 (ChartPanel.addCandleToAll 에 전달용). */
    public java.util.Map<String, Long> getStocks() {
        return java.util.Collections.unmodifiableMap(stocks);
    }

    /**
     * 모든 종목의 가격을 ChartPanel 캐시 기준으로 일괄 업데이트합니다.
     * 턴이 넘어갈 때 Main.java에서 호출합니다.
     */
    public void updateAllPrices(ChartPanel cp) {
        for (java.util.Map.Entry<String, Long> entry : stocks.entrySet()) {
            String name = entry.getKey();
            long newPrice = cp.getLastClosePriceFor(name);
            if (newPrice > 0) {
                updatePrice(name, newPrice);
            }
        }
    }

    public void updatePrice(String stockName, long price) {
        if (!stocks.containsKey(stockName)) return;

        stocks.put(stockName, price);

        // ── 등락률: ChartPanel 캔들 데이터 기준 (차트 헤더와 동일한 계산식) ──
        double changePct;
        if (chartPanel != null) {
            changePct = chartPanel.getChangePercentFor(stockName, price);
        } else {
            long oldPrice = prevPrices.getOrDefault(stockName, price);
            changePct = (oldPrice != 0) ? (price - oldPrice) / (double) oldPrice * 100.0 : 0.0;
        }
        prevPrices.put(stockName, price);

        boolean bull = changePct >= 0;
        String sign = bull ? "+" : "";
        String colorHex = bull ? "#ff6b6b" : "#4A9EFF";
        String bgColor  = bull ? "rgba(232,83,74,0.15)" : "rgba(74,158,255,0.15)";

        for (var node : rowBox.getChildren()) {
            if (node instanceof HBox row && stockName.equals(row.getUserData())) {
                Circle trendDot = (Circle) row.getChildren().get(0);
                VBox rightBox = (VBox) row.getChildren().get(3);
                Label priceLabel = (Label) rightBox.getChildren().get(0);
                Label changeLabel = (Label) rightBox.getChildren().get(1);

                Color trendColor = Color.web(colorHex);
                trendDot.setFill(trendColor);
                trendDot.setEffect(new DropShadow(8, trendColor));

                priceLabel.setText(formatMoney(price) + " 원");
                priceLabel.setTextFill(Color.web(colorHex));

                changeLabel.setText(String.format("%s%.2f%%", sign, changePct));
                changeLabel.setStyle(
                    "-fx-text-fill: " + colorHex + ";" +
                    "-fx-font-family: 'SUIT';" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 11px;" +
                    "-fx-padding: 2 6 2 6;" +
                    "-fx-background-color: " + bgColor + ";" +
                    "-fx-background-radius: 5;"
                );
                break;
            }
        }
    }

    private HBox buildStockRow(String name, long price) {
        Color trendColor = getTrendColor(name, price);

        Circle trendDot = new Circle(5);
        trendDot.setFill(trendColor);
        trendDot.setStroke(Color.web("#FFFFFF", 0.35));
        trendDot.setStrokeWidth(1);
        trendDot.setEffect(new DropShadow(8, trendColor));

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("SUIT", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#E0E8FF"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean bull = getTrendBool(name, price);
        String colorHex = bull ? "#ff6b6b" : "#4A9EFF";
        String bgColor  = bull ? "rgba(232,83,74,0.15)" : "rgba(74,158,255,0.15)";

        Label priceLabel = new Label(formatMoney(price) + " 원");
        priceLabel.setFont(Font.font("SUIT", FontWeight.BOLD, 14));
        priceLabel.setTextFill(Color.web(colorHex));

        Label changeLabel = new Label("+0.00%");
        changeLabel.setStyle(
            "-fx-text-fill: " + colorHex + ";" +
            "-fx-font-family: 'SUIT';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 11px;" +
            "-fx-padding: 2 6 2 6;" +
            "-fx-background-color: " + bgColor + ";" +
            "-fx-background-radius: 5;"
        );

        VBox rightBox = new VBox(2, priceLabel, changeLabel);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(8, trendDot, nameLabel, spacer, rightBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setUserData(name);
        applyRowStyle(row, false);

        row.setOnMouseEntered(e -> {
            if (row != selectedRow) applyRowStyle(row, true);
        });
        row.setOnMouseExited(e -> {
            if (row != selectedRow) applyRowStyle(row, false);
        });
        row.setOnMouseClicked(e -> selectRow(row, name, price));

        return row;
    }

    private void selectRow(HBox row, String name, long price) {
        if (selectedRow != null) applyRowStyle(selectedRow, false);
        selectedRow = row;
        applyRowSelectedStyle(row);

        if (onStockSelected != null) {
            onStockSelected.accept(name, price);
        }
    }

    private Color getTrendColor(String stockName, long price) {
        return getTrendBool(stockName, price) ? Color.web("#ff6b6b") : Color.web("#4A9EFF");
    }

    private boolean getTrendBull(String stockName, long price) {
        return getTrendBool(stockName, price);
    }

    private boolean getTrendBool(String stockName, long price) {
        return (chartPanel != null)
                ? chartPanel.isBullTrendFor(stockName, price)
                : ChartPanel.isBullTrend(stockName, price);
    }

    private void applyRowStyle(HBox row, boolean hover) {
        row.setStyle(
            "-fx-background-color: rgba(255,255,255," + (hover ? "0.12" : "0.06") + ");" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;"
        );
    }

    private void applyRowSelectedStyle(HBox row) {
        row.setStyle(
            "-fx-background-color: rgba(126,221,255,0.20);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(126,221,255,0.55);" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.2;" +
            "-fx-cursor: hand;"
        );
    }

    /**
     * 목록의 첫 번째 종목을 자동 선택합니다.
     * 앱 시작 시 ChartPanel과 초기 상태를 동기화하기 위해 Main.java에서 호출됩니다.
     */
    public void selectFirst() {
        if (rowBox.getChildren().isEmpty()) return;
        if (rowBox.getChildren().get(0) instanceof HBox row) {
            String name = (String) row.getUserData();
            Long price = stocks.get(name);
            if (price != null) {
                selectRow(row, name, price);
            }
        }
    }

    private static String formatMoney(long value) {
        return String.format("%,d", value);
    }
}