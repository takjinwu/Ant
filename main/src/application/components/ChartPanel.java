package application.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Arrays;

/**
 * ChartPanel — 화면설계서 v1 반영
 *
 * 헤더 구성 (❸·❻)
 * LEFT : [SAMSUNG 뱃지] [종목명 + 현재가격(대형)]
 * RIGHT : [캔들 | 라인 토글 버튼]
 *
 * 차트 구성
 * 메인 : 캔들스틱 or 라인차트 + Y축 가격 레이블 (❶) + 십자커서 (❺) + OHLCV 툴팁 (❷)
 * 하단 : 거래량 스틱 (❹)
 */
public class ChartPanel extends VBox {

    private static final int AXIS_W = 72;
    private static final int PAD_L = 10;
    private static final int PAD_R = 4;
    private static final double VOL_RATIO = 0.22;
    private static final double HEAD_H = 72;
    private static final int VISIBLE_CANDLES = 30;  // 화면에 표시할 최대 캔들 수
    private static final double LIMIT_RATE = 0.30;  // 상한가/하한가 ±30%

    private static final double[][] SAMPLE_SERIES = {
        {60000, 65000, 58000, 62000, 900000},
        {62000, 67000, 61000, 66000, 1100000},
        {66000, 70000, 64000, 65000, 800000},
        {65000, 68000, 63000, 67500, 1200000},
        {67500, 72000, 66000, 71000, 1400000},
        {71000, 74000, 69000, 70000, 950000},
        {70000, 73000, 67000, 68000, 1050000},
        {68000, 69000, 63000, 64000, 1300000},
        {64000, 66000, 60300, 63700, 1600000},
        {63700, 68200, 59100, 61000, 1800000},
        {61000, 64000, 58000, 62500, 700000},
        {62500, 66000, 61000, 65000, 850000},
        {65000, 69000, 63000, 67000, 1100000},
        {67000, 71000, 65000, 70000, 1250000},
        {70000, 75000, 68000, 73000, 1450000},
        {73000, 78000, 71000, 76000, 1600000},
        {76000, 80000, 74000, 77000, 900000},
        {77000, 81000, 75000, 79000, 1000000},
        {79000, 83000, 77000, 81000, 1150000},
        {81000, 85000, 79000, 83000, 1300000},
    };

    private double[][] data = SAMPLE_SERIES;

    // ── 종목별 데이터 캐시 (버그 수정: 탭 전환 시 addCandle 데이터 유지) ──
    private final java.util.HashMap<String, double[][]> dataCache = new java.util.HashMap<>();
    private String currentStockName = "";

    private final double panelW;
    private final double candleH;
    private final double volH;

    private Canvas candleCanvas;
    private Canvas volCanvas;
    private Label badge;
    private Label nameLabel;
    private Label priceLabel;
    private Label changeLabel;   // 등락률 표시 레이블
    private Label tooltipBox;
    private boolean showCandle = true;
    private double mouseX = -1, mouseY = -1;

    public ChartPanel(double width, double height) {
        this.panelW = width;
        this.volH = Math.round((height - HEAD_H) * VOL_RATIO);
        this.candleH = height - HEAD_H - volH - 2;

        setSpacing(0);
        setPrefSize(width, height);
        setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-radius: 14;" +
            "-fx-border-width: 1;"
        );

        getChildren().addAll(buildHeader(), buildCandlePane(), buildSeparator(), buildVolPane());
        updateHeaderPriceFromLatestCandle();
        redraw();
    }

    private HBox buildHeader() {
        HBox header = new HBox(14);
        header.setPrefHeight(HEAD_H);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: rgba(255,255,255,0.11);" +
            "-fx-border-width: 0 0 1 0;"
        );

        badge = new Label("SAMSUNG");
        badge.setStyle(
            "-fx-background-color: #1428A0;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'SUIT';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 6 10 6 10;" +
            "-fx-background-radius: 5;"
        );

        nameLabel = new Label("삼성전자 005930");
        nameLabel.setStyle(
            "-fx-text-fill: rgba(255,255,255,0.75);" +
            "-fx-font-family: 'SUIT';" +
            "-fx-font-size: 12px;"
        );

        priceLabel = new Label("83,000원");
        priceLabel.setStyle(
            "-fx-text-fill: #ff6b6b;" +
            "-fx-font-family: 'SUIT';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 24px;"
        );

        changeLabel = new Label("+0.00%");
        changeLabel.setStyle(
            "-fx-text-fill: #ff6b6b;" +
            "-fx-font-family: 'SUIT';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 15px;" +
            "-fx-padding: 4 8 4 8;" +
            "-fx-background-color: rgba(232,83,74,0.15);" +
            "-fx-background-radius: 6;"
        );

        HBox priceRow = new HBox(10, priceLabel, changeLabel);
        priceRow.setAlignment(Pos.CENTER_LEFT);

        VBox namePrice = new VBox(2, nameLabel, priceRow);
        namePrice.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleGroup tg = new ToggleGroup();
        ToggleButton candleBtn = new ToggleButton("캔들");
        ToggleButton lineBtn = new ToggleButton("라인");
        candleBtn.setToggleGroup(tg);
        lineBtn.setToggleGroup(tg);
        candleBtn.setSelected(true);

        String btnBase =
            "-fx-font-family: 'SUIT';" +
            "-fx-font-size: 13px;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 6 16 6 16;";
        String activeStyle = btnBase + "-fx-background-color: rgba(255,255,255,0.28); -fx-text-fill: white;";
        String inactiveStyle = btnBase + "-fx-background-color: rgba(255,255,255,0.09); -fx-text-fill: rgba(255,255,255,0.55);";

        candleBtn.setStyle(activeStyle);
        lineBtn.setStyle(inactiveStyle);

        candleBtn.selectedProperty().addListener((obs, o, n) -> {
            showCandle = n;
            candleBtn.setStyle(n ? activeStyle : inactiveStyle);
            lineBtn.setStyle(n ? inactiveStyle : activeStyle);
            redraw();
        });

        HBox toggleBox = new HBox(6, candleBtn, lineBtn);
        toggleBox.setAlignment(Pos.CENTER);

        header.getChildren().addAll(badge, namePrice, spacer, toggleBox);
        return header;
    }

    private StackPane buildCandlePane() {
        candleCanvas = new Canvas(panelW, candleH);

        tooltipBox = new Label();
        tooltipBox.setVisible(false);
        tooltipBox.setStyle(
            "-fx-background-color: rgba(20,24,50,0.92);" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'SUIT';" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 10 14 10 14;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(255,255,255,0.22);" +
            "-fx-border-radius: 8;" +
            "-fx-line-spacing: 3;"
        );

        StackPane pane = new StackPane(candleCanvas, tooltipBox);
        StackPane.setAlignment(tooltipBox, Pos.TOP_LEFT);

        candleCanvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            mouseX = e.getX();
            mouseY = e.getY();
            redraw();
            updateTooltip(e.getX(), e.getY());
        });

        candleCanvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            mouseX = -1;
            mouseY = -1;
            tooltipBox.setVisible(false);
            updateHeaderPriceFromLatestCandle();
            redraw();
        });

        return pane;
    }

    private Separator buildSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.10);");
        return sep;
    }

    private StackPane buildVolPane() {
        volCanvas = new Canvas(panelW, volH);

        volCanvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            mouseX = e.getX();
            mouseY = -1;
            redraw();
        });

        volCanvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            mouseX = -1;
            redraw();
        });

        return new StackPane(volCanvas);
    }

    public void applyNewsEffect(double effect) {
        System.out.println("[ChartPanel] 뉴스 효과: " + effect + "%");
        redraw();
    }

    /**
     * 턴이 넘어갈 때 호출 — 뉴스 effect(정수 %)를 반영한 새 캔들을 data 배열 끝에 추가합니다.
     * effect > 0 이면 상승 편향, effect < 0 이면 하락 편향.
     */
    public void addCandle(int effect) {
        if (data == null || data.length == 0) return;

        double[] last = data[data.length - 1];
        double prevClose = last[3];

        // effect를 ±% 드리프트로 변환 (최대 ±10% 수준)
        java.util.Random r = new java.util.Random();
        double bias   = effect * 0.004;                          // effect 10 → +4% 편향
        double drift  = bias + (r.nextDouble() - 0.5) * 0.04;   // 랜덤 노이즈 ±2%
        double open   = prevClose;
        double close  = open * (1 + drift);

        // ── 상한가/하한가 ±30% 클램프 (직전 종가 기준) ──
        double upperLimit = prevClose * (1 + LIMIT_RATE);   // 상한가
        double lowerLimit = prevClose * (1 - LIMIT_RATE);   // 하한가
        close = Math.max(lowerLimit, Math.min(upperLimit, close));
        double high   = Math.min(Math.max(open, close) * (1 + r.nextDouble() * 0.015), upperLimit);
        double low    = Math.max(Math.min(open, close) * (1 - r.nextDouble() * 0.015), lowerLimit);
        double vol    = 500_000 + r.nextDouble() * 1_500_000;

        // 호가단위 반올림
        open  = roundToTick(open);
        close = roundToTick(close);
        high  = roundToTick(high);
        low   = roundToTick(low);

        // data 배열에 새 캔들 추가
        double[][] newData = new double[data.length + 1][5];
        System.arraycopy(data, 0, newData, 0, data.length);
        newData[data.length] = new double[]{open, high, low, close, vol};
        data = newData;

        // 캐시 동기화 — 다른 종목을 클릭했다 돌아와도 누적 캔들 유지
        if (!currentStockName.isEmpty()) {
            dataCache.put(currentStockName, data);
        }

        updateHeaderPriceFromLatestCandle();
        redraw();
    }


    /**
     * 턴이 넘어갈 때 호출 — stocksMap에 있는 모든 종목의 캐시 데이터에 새 캔들을 추가합니다.
     * 아직 한 번도 클릭하지 않은 종목도 미리 초기화하여 캔들을 누적합니다.
     *
     * @param stocksMap 종목명 → 초기가격 맵 (StockListPanel.getStocks() 에서 전달)
     * @param effect    뉴스 효과 (정수 %)
     */
    public void addCandleToAll(java.util.Map<String, Long> stocksMap, int effect) {
        java.util.Random r = new java.util.Random();

        for (java.util.Map.Entry<String, Long> entry : stocksMap.entrySet()) {
            String name  = entry.getKey();
            long   price = entry.getValue();

            // 캐시에 없으면 초기 시리즈 생성
            double[][] stockData = dataCache.computeIfAbsent(name, k -> generateSeries(k, price));

            double[] last      = stockData[stockData.length - 1];
            double   prevClose = last[3];

            double bias  = effect * 0.004;
            double drift = bias + (r.nextDouble() - 0.5) * 0.04;
            double open  = prevClose;
            double close = open  * (1 + drift);

            // ── 상한가/하한가 ±30% 클램프 (직전 종가 기준) ──
            double upperLimit = prevClose * (1 + LIMIT_RATE);
            double lowerLimit = prevClose * (1 - LIMIT_RATE);
            close = Math.max(lowerLimit, Math.min(upperLimit, close));
            double high  = Math.min(Math.max(open, close) * (1 + r.nextDouble() * 0.015), upperLimit);
            double low   = Math.max(Math.min(open, close) * (1 - r.nextDouble() * 0.015), lowerLimit);
            double vol   = 500_000 + r.nextDouble() * 1_500_000;

            open  = roundToTick(open);
            close = roundToTick(close);
            high  = roundToTick(high);
            low   = roundToTick(low);

            double[][] newData = new double[stockData.length + 1][5];
            System.arraycopy(stockData, 0, newData, 0, stockData.length);
            newData[stockData.length] = new double[]{open, high, low, close, vol};
            dataCache.put(name, newData);

            // 현재 보고 있는 종목이면 화면 데이터도 동기화
            if (name.equals(currentStockName)) {
                data = newData;
            }
        }

        updateHeaderPriceFromLatestCandle();
        redraw();
    }

    /**
     * dataCache 기준으로 특정 종목의 마지막 종가를 반환합니다.
     * 캐시에 없으면 0을 반환합니다.
     */
    public long getLastClosePriceFor(String name) {
        double[][] d = dataCache.get(name);
        if (d == null || d.length == 0) return 0L;
        return (long) d[d.length - 1][3];
    }

    /**
     * 특정 종목의 최신 등락률(%)을 반환합니다.
     * 직전 캔들 종가 기준 turn-over-turn 변화율.
     */
    public double getChangePercentFor(String name, long price) {
        double[][] d = dataCache.computeIfAbsent(name, k -> generateSeries(k, price));
        if (d == null || d.length == 0) return 0.0;
        double[] last = d[d.length - 1];
        double prevClose = (d.length >= 2) ? d[d.length - 2][3] : last[0];
        if (prevClose == 0) return 0.0;
        return (last[3] - prevClose) / prevClose * 100.0;
    }

    /**
     * 상한가(+30%) 도달 여부를 반환합니다.
     */
    public boolean isUpperLimitFor(String name, long price) {
        double[][] d = dataCache.get(name);
        if (d == null || d.length < 2) return false;
        double prevClose = d[d.length - 2][3];
        return prevClose > 0 && d[d.length - 1][3] >= prevClose * (1 + LIMIT_RATE) * 0.999;
    }

    /**
     * 하한가(-30%) 도달 여부를 반환합니다.
     */
    public boolean isLowerLimitFor(String name, long price) {
        double[][] d = dataCache.get(name);
        if (d == null || d.length < 2) return false;
        double prevClose = d[d.length - 2][3];
        return prevClose > 0 && d[d.length - 1][3] <= prevClose * (1 - LIMIT_RATE) * 1.001;
    }

    public void showStock(String name, long price) {
        this.currentStockName = name;
        // 캐시에 없을 때만 새로 생성 — 기존 addCandle 데이터를 보존
        this.data = dataCache.computeIfAbsent(name, k -> generateSeries(k, price));

        boolean darkText = "카카오".equals(name);
        badge.setText(name);
        badge.setStyle(
            "-fx-background-color: " + brandColor(name) + ";" +
            "-fx-text-fill: " + (darkText ? "#1A1A1A" : "white") + ";" +
            "-fx-font-family: 'SUIT';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 6 10 6 10;" +
            "-fx-background-radius: 5;"
        );

        nameLabel.setText(name);

        mouseX = -1;
        mouseY = -1;
        if (tooltipBox != null) tooltipBox.setVisible(false);

        updateHeaderPriceFromLatestCandle();
        redraw();
    }

    private static double[][] generateSeries(String name, long price) {
        final int n = SAMPLE_SERIES.length;
        double[][] d = new double[n][5];
        java.util.Random r = new java.util.Random(name.hashCode() * 31L + price);

        double cur = price * (0.80 + r.nextDouble() * 0.30);
        for (int i = 0; i < n; i++) {
            double open = cur;
            double drift = (r.nextDouble() - 0.45) * 0.06;
            double close = open * (1 + drift);
            double high = Math.max(open, close) * (1 + r.nextDouble() * 0.025);
            double low = Math.min(open, close) * (1 - r.nextDouble() * 0.025);
            double vol = 500_000 + r.nextDouble() * 1_500_000;
            d[i] = new double[]{open, high, low, close, vol};
            cur = close;
        }

        double factor = price / d[n - 1][3];
        for (double[] row : d) {
            row[0] *= factor;
            row[1] *= factor;
            row[2] *= factor;
            row[3] *= factor;
        }

        // ── 호가단위 반올림 적용 ──
        for (double[] row : d) {
            row[0] = roundToTick(row[0]);
            row[1] = roundToTick(row[1]);
            row[2] = roundToTick(row[2]);
            row[3] = roundToTick(row[3]);
        }

        return d;
    }

    /**
     * 한국 주식시장 호가단위 규칙에 따라 가격을 반올림합니다.
     *   2,000원 미만         → 1원
     *   2,000 ~ 5,000원 미만 → 5원
     *   5,000 ~ 20,000원 미만→ 10원
     *  20,000 ~ 50,000원 미만→ 50원
     *  50,000 ~ 200,000원 미만→ 100원
     * 200,000 ~ 500,000원 미만→ 500원
     * 500,000원 이상         → 1,000원
     */
    private static double roundToTick(double price) {
        long tick;
        if      (price <   2_000) tick = 1;
        else if (price <   5_000) tick = 5;
        else if (price <  20_000) tick = 10;
        else if (price <  50_000) tick = 50;
        else if (price < 200_000) tick = 100;
        else if (price < 500_000) tick = 500;
        else                      tick = 1_000;
        return Math.round(price / tick) * tick;
    }

    public static boolean isBullTrend(String name, long price) {
        double[][] series = generateSeries(name, price);
        double[] last = series[series.length - 1];
        return last[3] >= last[0];
    }

    /** 현재 선택된 종목명 반환 */
    public String getCurrentStockName() { return currentStockName; }

    /** 현재 차트 마지막 캔들의 종가 반환 */
    public long getLastClosePrice() {
        if (data == null || data.length == 0) return 0L;
        return (long) data[data.length - 1][3];
    }

    /**
     * 캐시된 실제 데이터 기반으로 bull/bear 판단.
     * addCandle 이후에도 최신 트렌드를 정확히 반영합니다.
     */
    public boolean isBullTrendFor(String name, long price) {
        double[][] d = dataCache.computeIfAbsent(name, k -> generateSeries(k, price));
        double[] last = d[d.length - 1];
        return last[3] >= last[0];
    }

    private void updateHeaderPriceFromLatestCandle() {
        if (data == null || data.length == 0) return;

        double[] last = data[data.length - 1];

        // ── 등락률: 직전 캔들 종가 기준 (turn-over-turn) ──
        double prevClose = (data.length >= 2) ? data[data.length - 2][3] : last[0];
        double changePct = (prevClose != 0) ? (last[3] - prevClose) / prevClose * 100.0 : 0.0;
        boolean bull = changePct >= 0;

        // ── 상한가/하한가 도달 판정 ──
        boolean isUpperLimit = (prevClose > 0) && (last[3] >= prevClose * (1 + LIMIT_RATE) * 0.999);
        boolean isLowerLimit = (prevClose > 0) && (last[3] <= prevClose * (1 - LIMIT_RATE) * 1.001);

        String sign = changePct >= 0 ? "+" : "";
        String colorHex = bull ? "#ff6b6b" : "#4A9EFF";
        String bgColor  = bull ? "rgba(232,83,74,0.15)" : "rgba(74,158,255,0.15)";
        String limitBg  = isUpperLimit ? "rgba(255,50,50,0.35)" : "rgba(50,100,255,0.35)";

        priceLabel.setText(String.format("%,.0f원", last[3]));
        priceLabel.setStyle(
            "-fx-text-fill: " + colorHex + ";" +
            "-fx-font-family: 'SUIT';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 24px;"
        );

        if (isUpperLimit) {
            changeLabel.setText(String.format("+%.2f%%  ▲ 상한가", changePct));
            changeLabel.setStyle(
                "-fx-text-fill: #FF4040;" +
                "-fx-font-family: 'SUIT';" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 4 10 4 10;" +
                "-fx-background-color: " + limitBg + ";" +
                "-fx-background-radius: 6;" +
                "-fx-border-color: rgba(255,80,80,0.6);" +
                "-fx-border-radius: 6;" +
                "-fx-border-width: 1;"
            );
        } else if (isLowerLimit) {
            changeLabel.setText(String.format("%.2f%%  ▼ 하한가", changePct));
            changeLabel.setStyle(
                "-fx-text-fill: #4A9EFF;" +
                "-fx-font-family: 'SUIT';" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 4 10 4 10;" +
                "-fx-background-color: " + limitBg + ";" +
                "-fx-background-radius: 6;" +
                "-fx-border-color: rgba(80,120,255,0.6);" +
                "-fx-border-radius: 6;" +
                "-fx-border-width: 1;"
            );
        } else {
            changeLabel.setText(String.format("%s%.2f%%", sign, changePct));
            changeLabel.setStyle(
                "-fx-text-fill: " + colorHex + ";" +
                "-fx-font-family: 'SUIT';" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 15px;" +
                "-fx-padding: 4 8 4 8;" +
                "-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 6;"
            );
        }
    }

    private static String brandColor(String name) {
        switch (name) {
            case "삼성전자": return "#1428A0";
            case "SK하이닉스": return "#EA002C";
            case "셀트리온": return "#00A0E9";
            case "유한양행": return "#0067AC";
            case "한화에어로스페이스": return "#F37321";
            case "LIG넥스원": return "#003DA5";
            case "LG에너지솔루션": return "#A50034";
            case "에코프로비엠": return "#0BA14B";
            case "네이버": return "#03C75A";
            case "카카오": return "#FEE500";
            case "현대차": return "#002C5F";
            case "기아": return "#05141F";
            default: return "#3A4A8A";
        }
    }

    /** 전체 data 중 최근 VISIBLE_CANDLES 개만 반환 (슬라이딩 윈도우) */
    private double[][] getVisibleData() {
        if (data.length <= VISIBLE_CANDLES) return data;
        return java.util.Arrays.copyOfRange(data, data.length - VISIBLE_CANDLES, data.length);
    }

    private void redraw() {
        drawCandleChart();
        drawVolChart();
    }

    private double[] getPriceRange() {
        double minP = Double.MAX_VALUE, maxP = -Double.MAX_VALUE;
        for (double[] d : data) {
            minP = Math.min(minP, d[2]);
            maxP = Math.max(maxP, d[1]);
        }
        double margin = (maxP - minP) * 0.06;
        return new double[]{minP - margin, maxP + margin};
    }

    private double priceToY(double price, double minP, double maxP, double h) {
        return h - (price - minP) / (maxP - minP) * h;
    }

    private void drawCandleChart() {
        GraphicsContext gc = candleCanvas.getGraphicsContext2D();
        double cw = candleCanvas.getWidth(), ch = candleCanvas.getHeight();
        double[][] visible = getVisibleData();   // 최근 N개만 사용
        int n = visible.length;

        gc.clearRect(0, 0, cw, ch);

        double minP = Double.MAX_VALUE, maxP = -Double.MAX_VALUE;
        for (double[] d : visible) { minP = Math.min(minP, d[2]); maxP = Math.max(maxP, d[1]); }
        double margin = (maxP - minP) * 0.06;
        minP -= margin; maxP += margin;

        double chartW = cw - AXIS_W - PAD_L - PAD_R;
        double barW = chartW / n;
        double candleW = Math.max(barW * 0.55, 4);

        gc.setFont(Font.font("SUIT", 10));
        gc.setFill(Color.web("#8899aa"));
        gc.setStroke(Color.web("#ffffff", 0.07));
        gc.setLineWidth(1);
        for (int i = 0; i <= 5; i++) {
            double y = ch / 5.0 * i;
            gc.strokeLine(PAD_L, y, cw - AXIS_W, y);
            long labelPrice = Math.round((maxP - (maxP - minP) * i / 5) / 100.0) * 100;
            gc.fillText(String.format("%,d", labelPrice), cw - AXIS_W + 6, y + 4);
        }

        if (showCandle) {
            for (int i = 0; i < n; i++) {
                double open = visible[i][0];
                double high = visible[i][1];
                double low = visible[i][2];
                double close = visible[i][3];
                boolean bull = close >= open;

                Color body = bull ? Color.web("#E8534A") : Color.web("#4A9EFF");
                Color wick = bull ? Color.web("#E8534A", 0.8) : Color.web("#4A9EFF", 0.8);

                double cx = PAD_L + (i + 0.5) * barW;
                double bTop = priceToY(Math.max(open, close), minP, maxP, ch);
                double bBot = priceToY(Math.min(open, close), minP, maxP, ch);

                gc.setStroke(wick);
                gc.setLineWidth(1.5);
                gc.strokeLine(cx, priceToY(high, minP, maxP, ch), cx, bTop);
                gc.strokeLine(cx, bBot, cx, priceToY(low, minP, maxP, ch));

                gc.setFill(body);
                gc.fillRect(cx - candleW / 2, bTop, candleW, Math.max(bBot - bTop, 1.5));
            }
        } else {
            gc.setStroke(Color.web("#4A9EFF"));
            gc.setLineWidth(2);

            gc.beginPath();
            gc.moveTo(PAD_L + 0.5 * barW, ch);
            for (int i = 0; i < n; i++) {
                gc.lineTo(PAD_L + (i + 0.5) * barW, priceToY(visible[i][3], minP, maxP, ch));
            }
            gc.lineTo(PAD_L + (n - 0.5) * barW, ch);
            gc.closePath();
            gc.setFill(Color.web("#4A9EFF", 0.10));
            gc.fill();

            gc.beginPath();
            for (int i = 0; i < n; i++) {
                double x = PAD_L + (i + 0.5) * barW;
                double y = priceToY(visible[i][3], minP, maxP, ch);
                if (i == 0) gc.moveTo(x, y);
                else gc.lineTo(x, y);
            }
            gc.stroke();
        }

        if (mouseX >= 0 && mouseX <= cw - AXIS_W) {
            gc.setStroke(Color.web("#ffffff", 0.38));
            gc.setLineWidth(1);
            gc.setLineDashes(4, 4);
            gc.strokeLine(mouseX, 0, mouseX, ch);

            if (mouseY >= 0 && mouseY <= ch) {
                gc.strokeLine(0, mouseY, cw - AXIS_W, mouseY);

                double curPrice = maxP - (mouseY / ch) * (maxP - minP);
                gc.setLineDashes();
                gc.setFill(Color.web("#ffffff", 0.92));
                gc.fillRoundRect(cw - AXIS_W, mouseY - 10, AXIS_W - 2, 20, 5, 5);
                gc.setFill(Color.web("#06124A"));
                gc.setFont(Font.font("SUIT", FontWeight.BOLD, 10));
                gc.fillText(String.format("%,d", (long) curPrice), cw - AXIS_W + 4, mouseY + 4);
            }
            gc.setLineDashes();
        }
    }

    private void drawVolChart() {
        GraphicsContext gc = volCanvas.getGraphicsContext2D();
        double cw = volCanvas.getWidth(), ch = volCanvas.getHeight();
        double[][] visible = getVisibleData();   // 최근 N개만 사용
        int n = visible.length;

        gc.clearRect(0, 0, cw, ch);

        double barW = (cw - AXIS_W - PAD_L - PAD_R) / n;
        double maxVol = Arrays.stream(visible).mapToDouble(d -> d[4]).max().orElse(1);

        gc.setStroke(Color.web("#ffffff", 0.06));
        gc.setLineWidth(1);
        for (int i = 0; i <= 2; i++) {
            double y = ch / 2.0 * i;
            gc.strokeLine(PAD_L, y, cw - AXIS_W, y);
        }

        for (int i = 0; i < n; i++) {
            boolean bull = visible[i][3] >= visible[i][0];
            gc.setFill(bull ? Color.web("#E8534A", 0.75) : Color.web("#4A9EFF", 0.75));
            double bh = (visible[i][4] / maxVol) * (ch - 4);
            double x = PAD_L + i * barW + barW * 0.20;
            gc.fillRect(x, ch - bh, Math.max(barW * 0.60, 2), bh);
        }

        gc.setFill(Color.web("#8899aa"));
        gc.setFont(Font.font("SUIT", 10));
        gc.fillText(String.format("%,.0f", maxVol), cw - AXIS_W + 4, 12);
        gc.fillText(String.format("%,.0f", maxVol / 2), cw - AXIS_W + 4, ch / 2 + 4);

        if (mouseX >= 0 && mouseX <= cw - AXIS_W) {
            gc.setStroke(Color.web("#ffffff", 0.28));
            gc.setLineWidth(1);
            gc.setLineDashes(4, 4);
            gc.strokeLine(mouseX, 0, mouseX, ch);
            gc.setLineDashes();
        }
    }

    private void updateTooltip(double mx, double my) {
        double[][] visible = getVisibleData();
        double cw = candleCanvas.getWidth();
        double barW = (cw - AXIS_W - PAD_L - PAD_R) / visible.length;
        int idx = (int) ((mx - PAD_L) / barW);
        if (idx < 0 || idx >= visible.length) {
            tooltipBox.setVisible(false);
            return;
        }

        double[] d = visible[idx];

        tooltipBox.setText(String.format(
            "시가: %,.0f\n고가: %,.0f\n저가: %,.0f\n종가: %,.0f\n거래량: %,.0f",
            d[0], d[1], d[2], d[3], d[4]
        ));
        tooltipBox.setVisible(true);

        double tx = mx + 14, ty = my + 14;
        if (tx + 150 > cw - AXIS_W) tx = mx - 160;
        if (ty + 110 > candleH) ty = my - 120;
        StackPane.setMargin(tooltipBox, new Insets(ty, 0, 0, tx));

        boolean bull = d[3] >= d[0];
        double changePct = (d[0] != 0) ? (d[3] - d[0]) / d[0] * 100.0 : 0.0;
        String sign = changePct >= 0 ? "+" : "";
        String colorHex = bull ? "#ff6b6b" : "#4A9EFF";
        String bgColor  = bull ? "rgba(232,83,74,0.15)" : "rgba(74,158,255,0.15)";

        priceLabel.setText(String.format("%,.0f원", d[3]));
        priceLabel.setStyle(
            "-fx-text-fill: " + colorHex + ";" +
            "-fx-font-family: 'SUIT';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 24px;"
        );

        changeLabel.setText(String.format("%s%.2f%%", sign, changePct));
        changeLabel.setStyle(
            "-fx-text-fill: " + colorHex + ";" +
            "-fx-font-family: 'SUIT';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 15px;" +
            "-fx-padding: 4 8 4 8;" +
            "-fx-background-color: " + bgColor + ";" +
            "-fx-background-radius: 6;"
        );
    }
}