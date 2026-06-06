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
    private final java.util.Set<String> delistedStocks = new java.util.HashSet<>();  // 상장폐지 종목
    private BiConsumer<String, Long> onStockSelected = null;
    private java.util.function.Consumer<String> onDelisted = null;  // 상장폐지 이벤트 콜백

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
        header.setFont(Font.font("SUIT", FontWeight.EXTRA_BOLD, 20));
        header.setTextFill(Color.web("#FFFFFF"));

        Label nameCol = new Label("종목명");
        nameCol.setFont(Font.font("SUIT", FontWeight.BOLD, 14));
 
        nameCol.setTextFill(Color.web("#AAB4D4"));

        Region headSpacer = new Region();
        HBox.setHgrow(headSpacer, Priority.ALWAYS);

        Label priceCol = new Label("현재가");
        priceCol.setFont(Font.font("SUIT", FontWeight.BOLD, 14));
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


    /** 전체 종목 맵을 반환합니다 (ChartPanel.addCandleToAll 에 전달용). 상장폐지 종목 제외. */
    public java.util.Map<String, Long> getStocks() {
        java.util.Map<String, Long> active = new java.util.LinkedHashMap<>();
        for (var entry : stocks.entrySet()) {
            if (!delistedStocks.contains(entry.getKey())) {
                active.put(entry.getKey(), entry.getValue());
            }
        }
        return java.util.Collections.unmodifiableMap(active);
    }

    /** 상장폐지 이벤트 리스너 등록 */
    public void setOnDelisted(java.util.function.Consumer<String> listener) {
        this.onDelisted = listener;
    }

    /** 해당 종목이 상장폐지 상태인지 반환 */
    public boolean isDelisted(String name) {
        return delistedStocks.contains(name);
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
        if (delistedStocks.contains(stockName)) return;  // 이미 상장폐지된 종목은 무시

        // ── 1,000원 미만 → 상장폐지 ──
        if (price < 1_000) {
            stocks.put(stockName, price);
            triggerDelist(stockName);
            return;
        }

        stocks.put(stockName, price);

        // ── 등락률: ChartPanel 직전 캔들 종가 기준 (turn-over-turn) ──
        double changePct;
        boolean upperLimit = false, lowerLimit = false;
        if (chartPanel != null) {
            changePct   = chartPanel.getChangePercentFor(stockName, price);
            upperLimit  = chartPanel.isUpperLimitFor(stockName, price);
            lowerLimit  = chartPanel.isLowerLimitFor(stockName, price);
        } else {
            long oldPrice = prevPrices.getOrDefault(stockName, price);
            changePct = (oldPrice != 0) ? (price - oldPrice) / (double) oldPrice * 100.0 : 0.0;
        }
        prevPrices.put(stockName, price);

        boolean bull = changePct >= 0;
        String colorHex = bull ? "#ff6b6b" : "#4A9EFF";

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

                // 상한가/하한가 뱃지
                if (upperLimit) {
                    changeLabel.setText(String.format("+%.2f%% ▲상한가", changePct));
                    changeLabel.setStyle(
                        "-fx-text-fill: #FF4040;" +
                        "-fx-font-family: 'SUIT';" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 10px;" +
                        "-fx-padding: 2 6 2 6;" +
                        "-fx-background-color: rgba(255,50,50,0.25);" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-color: rgba(255,80,80,0.55);" +
                        "-fx-border-radius: 5;" +
                        "-fx-border-width: 1;"
                    );
                } else if (lowerLimit) {
                    changeLabel.setText(String.format("%.2f%% ▼하한가", changePct));
                    changeLabel.setStyle(
                        "-fx-text-fill: #4A9EFF;" +
                        "-fx-font-family: 'SUIT';" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 10px;" +
                        "-fx-padding: 2 6 2 6;" +
                        "-fx-background-color: rgba(50,100,255,0.25);" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-color: rgba(80,130,255,0.55);" +
                        "-fx-border-radius: 5;" +
                        "-fx-border-width: 1;"
                    );
                } else {
                    String sign = bull ? "+" : "";
                    String bgColor = bull ? "rgba(232,83,74,0.15)" : "rgba(74,158,255,0.15)";
                    changeLabel.setText(String.format("%s%.2f%%", sign, changePct));
                    changeLabel.setStyle(
                        "-fx-text-fill: " + colorHex + ";" +
                        "-fx-font-family: 'SUIT';" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 2 6 2 6;" +
                        "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 5;"
                    );
                }
                break;
            }
        }
    }

    private HBox buildStockRow(String name, long price) {
        Color trendColor = getTrendColor(name, price);

        Circle trendDot = new Circle(6);
        trendDot.setFill(trendColor);
        trendDot.setStroke(Color.web("#FFFFFF", 0.35));
        trendDot.setStrokeWidth(1);
        trendDot.setEffect(new DropShadow(8, trendColor));

        Label nameLabel = new Label(name);
        // 폰트는 setFont() 대신 인라인 스타일로 지정한다.
        // setFont() 값은 스타일시트(.label / .root)의 -fx-font-family 와 충돌해,
        // hover/클릭으로 row.setStyle() 이 호출돼 CSS가 재적용될 때마다
        // 폰트가 다시 계산(재해석)되며 크기가 들쭉날쭉해진다.
        // 인라인 스타일은 우선순위가 가장 높아 재적용돼도 크기가 고정된다.
        nameLabel.setStyle("-fx-font-family: 'SUIT'; -fx-font-weight: bold; -fx-font-size: 16px;");
        nameLabel.setTextFill(Color.web("#E0E8FF"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean bull = getTrendBool(name, price);
        String colorHex = bull ? "#ff6b6b" : "#4A9EFF";
        String bgColor  = bull ? "rgba(232,83,74,0.15)" : "rgba(74,158,255,0.15)";

        Label priceLabel = new Label(formatMoney(price) + " 원");
        // nameLabel과 동일하게 인라인 스타일로 폰트 고정 (색상만 setTextFill 로 동적 변경)
        priceLabel.setStyle("-fx-font-family: 'SUIT'; -fx-font-weight: bold; -fx-font-size: 16px;");
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
        row.setPadding(new Insets(12, 14, 12, 14));
        row.setUserData(name);
        // 카드(행) 높이를 콘텐츠 기준으로 고정해 hover/클릭 시 절대 늘어나지 않게 한다.
        row.setMinHeight(Region.USE_PREF_SIZE);
        row.setMaxHeight(Region.USE_PREF_SIZE);
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

    /**
     * 상장폐지 처리 — 해당 종목 행을 비활성화하고 상장폐지 뱃지를 표시합니다.
     */
    private void triggerDelist(String name) {
        delistedStocks.add(name);

        for (var node : rowBox.getChildren()) {
            if (!(node instanceof HBox row)) continue;
            if (!name.equals(row.getUserData())) continue;

            // 클릭/호버 이벤트 제거
            row.setOnMouseClicked(null);
            row.setOnMouseEntered(null);
            row.setOnMouseExited(null);
            row.setStyle(
                "-fx-background-color: rgba(120,30,30,0.18);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: transparent;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1.2;" +
                "-fx-cursor: default;" +
                "-fx-opacity: 0.62;"
            );

            // 상태 점 → 회색
            Circle trendDot = (Circle) row.getChildren().get(0);
            trendDot.setFill(Color.web("#888888"));
            trendDot.setEffect(null);

            // 종목명 → 회색
            Label nameLabel = (Label) row.getChildren().get(1);
            nameLabel.setTextFill(Color.web("#999999"));

            // 가격/등락률 → 상장폐지 표시
            VBox rightBox = (VBox) row.getChildren().get(3);
            Label priceLabel  = (Label) rightBox.getChildren().get(0);
            Label changeLabel = (Label) rightBox.getChildren().get(1);

            priceLabel.setText("상장폐지");
            // 인라인 폰트 스타일과 충돌하지 않도록 setFont 대신 setStyle 사용
            priceLabel.setStyle("-fx-font-family: 'SUIT'; -fx-font-weight: bold; -fx-font-size: 13px;");
            priceLabel.setTextFill(Color.web("#FF5555"));

            changeLabel.setText("거래정지");
            changeLabel.setStyle(
                "-fx-text-fill: #FF5555;" +
                "-fx-font-family: 'SUIT';" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 10px;" +
                "-fx-padding: 2 6 2 6;" +
                "-fx-background-color: rgba(255,50,50,0.20);" +
                "-fx-background-radius: 5;" +
                "-fx-border-color: rgba(255,80,80,0.55);" +
                "-fx-border-radius: 5;" +
                "-fx-border-width: 1;"
            );
            break;
        }

        // 선택 중이던 행이 상장폐지되면 선택 해제
        if (selectedRow != null && name.equals(selectedRow.getUserData())) {
            selectedRow = null;
        }

        // 콜백 발화
        if (onDelisted != null) {
            javafx.application.Platform.runLater(() -> onDelisted.accept(name));
        }
    }

    private void selectRow(HBox row, String name, long price) {
        if (selectedRow != null) applyRowStyle(selectedRow, false);
        selectedRow = row;
        applyRowSelectedStyle(row);

        if (onStockSelected != null) {
            // 행 생성 시 캡처된 price 대신 stocks 맵에서 최신 가격을 조회
            long latestPrice = stocks.getOrDefault(name, price);
            onStockSelected.accept(name, latestPrice);
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
        // 테두리 두께(1.2)를 항상 동일하게 유지하고 색만 투명으로 둔다.
        // → 선택/호버 상태와 insets(레이아웃 크기)가 같아져 카드 크기가 변하지 않는다.
        row.setStyle(
            "-fx-background-color: rgba(255,255,255," + (hover ? "0.12" : "0.06") + ");" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: transparent;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.2;" +
            "-fx-cursor: hand;"
        );
    }

    private void applyRowSelectedStyle(HBox row) {
        // applyRowStyle 와 동일한 테두리 두께(1.2)를 사용해 크기 변화를 없앤다. 색만 강조색으로.
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