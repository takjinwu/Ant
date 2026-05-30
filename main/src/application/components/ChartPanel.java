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
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Arrays;

/**
 * ChartPanel — 화면설계서 v1 반영
 *
 * 헤더 구성 (❸·❻)
 *   LEFT  : [SAMSUNG 뱃지] [종목명 + 현재가격(대형)] [브랜드 원형 이미지]
 *   RIGHT : [캔들 | 라인 토글 버튼]
 *
 * 차트 구성
 *   메인  : 캔들스틱 or 라인차트 + Y축 가격 레이블 (❶) + 십자커서 (❺) + OHLCV 툴팁 (❷)
 *   하단  : 거래량 스틱 (❹)
 *
 * 삭제  : MA 5일/10일/60일선
 */
public class ChartPanel extends VBox {

    // ── 레이아웃 상수 ──────────────────────────────────────────────
    private static final int    AXIS_W    = 72;
    private static final int    PAD_L     = 10;
    private static final int    PAD_R     = 4;
    private static final double VOL_RATIO = 0.22;
    private static final double HEAD_H    = 72;

    // ── 샘플 데이터: open, high, low, close, volume ──────────────
    // TODO: 뉴스 이벤트 연결 시 이 배열을 외부 List<double[]>로 교체
    private static final double[][] SAMPLE_SERIES = {
        {60000, 65000, 58000, 62000,  900000},
        {62000, 67000, 61000, 66000, 1100000},
        {66000, 70000, 64000, 65000,  800000},
        {65000, 68000, 63000, 67500, 1200000},
        {67500, 72000, 66000, 71000, 1400000},
        {71000, 74000, 69000, 70000,  950000},
        {70000, 73000, 67000, 68000, 1050000},
        {68000, 69000, 63000, 64000, 1300000},
        {64000, 66000, 60300, 63700, 1600000},
        {63700, 68200, 59100, 61000, 1800000},
        {61000, 64000, 58000, 62500,  700000},
        {62500, 66000, 61000, 65000,  850000},
        {65000, 69000, 63000, 67000, 1100000},
        {67000, 71000, 65000, 70000, 1250000},
        {70000, 75000, 68000, 73000, 1450000},
        {73000, 78000, 71000, 76000, 1600000},
        {76000, 80000, 74000, 77000,  900000},
        {77000, 81000, 75000, 79000, 1000000},
        {79000, 83000, 77000, 81000, 1150000},
        {81000, 85000, 79000, 83000, 1300000},
    };

    /** 현재 표시 중인 종목의 캔들 시리즈 (종목 선택 시 교체됨) */
    private double[][] data = SAMPLE_SERIES;

    // ── 내부 상태 ──────────────────────────────────────────────────
    private final double panelW;
    private final double candleH;
    private final double volH;

    private Canvas  candleCanvas;
    private Canvas  volCanvas;
    private Label   badge;         // 종목 뱃지
    private Label   nameLabel;     // 종목명
    private Label   priceLabel;    // ❸ 현재가격 (대형)
    private Label   tooltipBox;    // ❷ OHLCV 툴팁
    private boolean showCandle = true;
    private double  mouseX = -1, mouseY = -1;

    // ── 생성자 ────────────────────────────────────────────────────
    public ChartPanel(double width, double height) {
        this.panelW  = width;
        this.volH    = Math.round((height - HEAD_H) * VOL_RATIO);
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
        redraw();
    }

    // ────────────────────────────────────────────────────────────
    //  ❸·❻  헤더
    //  LEFT  : [SAMSUNG 뱃지] [종목명 + 현재가] [브랜드 원형]
    //  RIGHT : [캔들 | 라인 버튼]
    // ────────────────────────────────────────────────────────────
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

        // ── 종목 뱃지 ──
        badge = new Label("SAMSUNG");
        badge.setStyle(
            "-fx-background-color: #1428A0;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: '맑은 고딕';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 6 10 6 10;" +
            "-fx-background-radius: 5;"
        );

        // ── 종목명 + 현재가격 (수직 배치) ──
        nameLabel = new Label("삼성전자  005930");
        nameLabel.setStyle(
            "-fx-text-fill: rgba(255,255,255,0.75);" +
            "-fx-font-family: '맑은 고딕';" +
            "-fx-font-size: 12px;"
        );

        priceLabel = new Label("83,000원");
        priceLabel.setStyle(
            "-fx-text-fill: #ff6b6b;" +
            "-fx-font-family: '맑은 고딕';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 24px;"
        );

        VBox namePrice = new VBox(2, nameLabel, priceLabel);
        namePrice.setAlignment(Pos.CENTER_LEFT);

        // ── ❻ 브랜드 원형 이미지 (실제 이미지 있으면 ImageView로 교체) ──
        Circle brandCircle = new Circle(18);
        brandCircle.setFill(Color.web("#E8534A", 0.85));
        brandCircle.setStroke(Color.web("#ffffff", 0.25));
        brandCircle.setStrokeWidth(1.5);

        // ── 스페이서 ──
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── ❻ 캔들 / 라인 토글 버튼 ──
        ToggleGroup tg = new ToggleGroup();
        ToggleButton candleBtn = new ToggleButton("캔들");
        ToggleButton lineBtn   = new ToggleButton("라인");
        candleBtn.setToggleGroup(tg);
        lineBtn.setToggleGroup(tg);
        candleBtn.setSelected(true);

        String btnBase =
            "-fx-font-family: '맑은 고딕';" +
            "-fx-font-size: 13px;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 6 16 6 16;";
        String activeStyle   = btnBase + "-fx-background-color: rgba(255,255,255,0.28); -fx-text-fill: white;";
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

        header.getChildren().addAll(badge, namePrice, brandCircle, spacer, toggleBox);
        return header;
    }

    // ────────────────────────────────────────────────────────────
    //  캔들 캔버스 영역 (❶ ❷ ❺)
    // ────────────────────────────────────────────────────────────
    private StackPane buildCandlePane() {
        candleCanvas = new Canvas(panelW, candleH);

        // ❷ OHLCV 툴팁박스
        tooltipBox = new Label();
        tooltipBox.setVisible(false);
        tooltipBox.setStyle(
            "-fx-background-color: rgba(20,24,50,0.92);" +
            "-fx-text-fill: white;" +
            "-fx-font-family: '맑은 고딕';" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 10 14 10 14;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(255,255,255,0.22);" +
            "-fx-border-radius: 8;" +
            "-fx-line-spacing: 3;"
        );

        StackPane pane = new StackPane(candleCanvas, tooltipBox);
        StackPane.setAlignment(tooltipBox, Pos.TOP_LEFT);

        // ❺ 마우스 이벤트 — 십자커서 + 툴팁
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
            redraw();
        });

        return pane;
    }

    private Separator buildSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.10);");
        return sep;
    }

    // ────────────────────────────────────────────────────────────
    //  ❹ 거래량 스틱 영역
    // ────────────────────────────────────────────────────────────
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

    // ────────────────────────────────────────────────────────────
    //  외부 API
    // ────────────────────────────────────────────────────────────
    /**
     * 뉴스 효과 수신. Main.java의 주석 해제 후 바로 사용 가능.
     * @param effect 퍼센트 단위 등락 (예: +5.0, -3.0)
     */
    public void applyNewsEffect(double effect) {
        // TODO: data를 외부 List<double[]>로 교체하고 여기서 append/update
        System.out.println("[ChartPanel] 뉴스 효과: " + effect + "%");
        redraw();
    }

    /**
     * 주식 목록에서 종목 선택 시 호출 — 헤더(뱃지/종목명/현재가)와 차트를 해당 종목으로 교체.
     * @param name  종목명
     * @param price 현재 주당 가격 (마지막 캔들 종가가 이 값에 맞춰짐)
     */
    public void showStock(String name, long price) {
        this.data = generateSeries(name, price);

        // ── 뱃지 ──
        boolean darkText = "카카오".equals(name);     // 카카오 노란색 → 어두운 글자
        badge.setText(name);
        badge.setStyle(
            "-fx-background-color: " + brandColor(name) + ";" +
            "-fx-text-fill: " + (darkText ? "#1A1A1A" : "white") + ";" +
            "-fx-font-family: '맑은 고딕';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 6 10 6 10;" +
            "-fx-background-radius: 5;"
        );

        // ── 종목명 ──
        nameLabel.setText(name);

        // ── 현재가 (상승=빨강 / 하락=파랑) ──
        boolean bull = data[data.length - 1][3] >= data[0][0];
        priceLabel.setText(String.format("%,d원", price));
        priceLabel.setStyle(
            "-fx-text-fill: " + (bull ? "#ff6b6b" : "#4A9EFF") + ";" +
            "-fx-font-family: '맑은 고딕';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 24px;"
        );

        // 커서/툴팁 초기화 후 다시 그림
        mouseX = -1;
        mouseY = -1;
        if (tooltipBox != null) tooltipBox.setVisible(false);
        redraw();
    }

    /**
     * 종목별 캔들 시리즈 생성.
     * ─ 종목명 기반 시드로 매번 동일한 차트가 재현되며, 마지막 종가는 price에 정확히 일치.
     * ─ 추후 실제 시세/뉴스 데이터가 생기면 이 메서드를 대체하면 됨.
     */
    private double[][] generateSeries(String name, long price) {
        final int n = SAMPLE_SERIES.length;
        double[][] d = new double[n][5];
        java.util.Random r = new java.util.Random(name.hashCode() * 31L + price);

        double cur = price * (0.80 + r.nextDouble() * 0.30);   // 시작가: 현재가의 80~110%
        for (int i = 0; i < n; i++) {
            double open  = cur;
            double drift = (r.nextDouble() - 0.45) * 0.06;      // ±6%, 소폭 상승 편향
            double close = open * (1 + drift);
            double high  = Math.max(open, close) * (1 + r.nextDouble() * 0.025);
            double low   = Math.min(open, close) * (1 - r.nextDouble() * 0.025);
            double vol   = 500_000 + r.nextDouble() * 1_500_000;
            d[i] = new double[]{open, high, low, close, vol};
            cur = close;
        }

        // 마지막 종가를 현재가에 정확히 맞추도록 가격축 전체 보정
        double factor = price / d[n - 1][3];
        for (double[] row : d) {
            row[0] *= factor;
            row[1] *= factor;
            row[2] *= factor;
            row[3] *= factor;
        }
        return d;
    }

    /** 종목별 뱃지 브랜드 컬러 */
    private static String brandColor(String name) {
        switch (name) {
            case "삼성전자":          return "#1428A0";
            case "SK하이닉스":        return "#EA002C";
            case "셀트리온":          return "#00A0E9";
            case "유한양행":          return "#0067AC";
            case "한화에어로스페이스": return "#F37321";
            case "LIG넥스원":         return "#003DA5";
            case "LG에너지솔루션":     return "#A50034";
            case "에코프로비엠":       return "#0BA14B";
            case "네이버":            return "#03C75A";
            case "카카오":            return "#FEE500";
            case "현대차":            return "#002C5F";
            case "기아":              return "#05141F";
            default:                  return "#3A4A8A";
        }
    }

    // ────────────────────────────────────────────────────────────
    //  렌더링
    // ────────────────────────────────────────────────────────────
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

    // ── ❶ 메인 캔들/라인 차트 ──────────────────────────────────
    private void drawCandleChart() {
        GraphicsContext gc = candleCanvas.getGraphicsContext2D();
        double cw = candleCanvas.getWidth(), ch = candleCanvas.getHeight();
        int n = data.length;

        gc.clearRect(0, 0, cw, ch);

        // 배경 투명 — 메인 그라데이션 배경이 그대로 비침

        double[] range = getPriceRange();
        double minP = range[0], maxP = range[1];
        double chartW  = cw - AXIS_W - PAD_L - PAD_R;
        double barW    = chartW / n;
        double candleW = Math.max(barW * 0.55, 4);

        // ❶ 격자 + Y축 가격 레이블
        gc.setFont(Font.font("맑은 고딕", 10));
        gc.setFill(Color.web("#8899aa"));
        gc.setStroke(Color.web("#ffffff", 0.07));
        gc.setLineWidth(1);
        for (int i = 0; i <= 5; i++) {
            double y = ch / 5.0 * i;
            gc.strokeLine(PAD_L, y, cw - AXIS_W, y);
            long labelPrice = Math.round((maxP - (maxP - minP) * i / 5) / 100.0) * 100;
            gc.fillText(String.format("%,d", labelPrice), cw - AXIS_W + 6, y + 4);
        }

        // ── 캔들 or 라인 ──
        if (showCandle) {
            for (int i = 0; i < n; i++) {
                double open  = data[i][0];
                double high  = data[i][1];
                double low   = data[i][2];
                double close = data[i][3];
                boolean bull = close >= open;

                Color body = bull ? Color.web("#E8534A") : Color.web("#4A9EFF");
                Color wick = bull ? Color.web("#E8534A", 0.8) : Color.web("#4A9EFF", 0.8);

                double cx   = PAD_L + (i + 0.5) * barW;
                double bTop = priceToY(Math.max(open, close), minP, maxP, ch);
                double bBot = priceToY(Math.min(open, close), minP, maxP, ch);

                // 심지
                gc.setStroke(wick);
                gc.setLineWidth(1.5);
                gc.strokeLine(cx, priceToY(high, minP, maxP, ch), cx, bTop);
                gc.strokeLine(cx, bBot, cx, priceToY(low, minP, maxP, ch));

                // 몸통
                gc.setFill(body);
                gc.fillRect(cx - candleW / 2, bTop, candleW, Math.max(bBot - bTop, 1.5));
            }
        } else {
            // 라인 차트 (면적 채우기 포함)
            gc.setStroke(Color.web("#4A9EFF"));
            gc.setLineWidth(2);

            gc.beginPath();
            gc.moveTo(PAD_L + 0.5 * barW, ch);
            for (int i = 0; i < n; i++)
                gc.lineTo(PAD_L + (i + 0.5) * barW, priceToY(data[i][3], minP, maxP, ch));
            gc.lineTo(PAD_L + (n - 0.5) * barW, ch);
            gc.closePath();
            gc.setFill(Color.web("#4A9EFF", 0.10));
            gc.fill();

            gc.beginPath();
            for (int i = 0; i < n; i++) {
                double x = PAD_L + (i + 0.5) * barW;
                double y = priceToY(data[i][3], minP, maxP, ch);
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();
        }

        // ❺ 십자 커서
        if (mouseX >= 0 && mouseX <= cw - AXIS_W) {
            gc.setStroke(Color.web("#ffffff", 0.38));
            gc.setLineWidth(1);
            gc.setLineDashes(4, 4);
            gc.strokeLine(mouseX, 0, mouseX, ch);

            if (mouseY >= 0 && mouseY <= ch) {
                gc.strokeLine(0, mouseY, cw - AXIS_W, mouseY);

                // Y축 강조 가격 라벨
                double curPrice = maxP - (mouseY / ch) * (maxP - minP);
                gc.setLineDashes();
                gc.setFill(Color.web("#ffffff", 0.92));
                gc.fillRoundRect(cw - AXIS_W, mouseY - 10, AXIS_W - 2, 20, 5, 5);
                gc.setFill(Color.web("#06124A"));
                gc.setFont(Font.font("맑은 고딕", FontWeight.BOLD, 10));
                gc.fillText(String.format("%,d", (long) curPrice), cw - AXIS_W + 4, mouseY + 4);
            }
            gc.setLineDashes();
        }
    }

    // ── ❹ 거래량 스틱 ────────────────────────────────────────────
    private void drawVolChart() {
        GraphicsContext gc = volCanvas.getGraphicsContext2D();
        double cw = volCanvas.getWidth(), ch = volCanvas.getHeight();
        int n = data.length;

        gc.clearRect(0, 0, cw, ch);
        // 배경 투명 — 메인 그라데이션 배경이 그대로 비침

        double barW   = (cw - AXIS_W - PAD_L - PAD_R) / n;
        double maxVol = Arrays.stream(data).mapToDouble(d -> d[4]).max().orElse(1);

        // 격자
        gc.setStroke(Color.web("#ffffff", 0.06));
        gc.setLineWidth(1);
        for (int i = 0; i <= 2; i++) {
            double y = ch / 2.0 * i;
            gc.strokeLine(PAD_L, y, cw - AXIS_W, y);
        }

        // 거래량 스틱
        for (int i = 0; i < n; i++) {
            boolean bull = data[i][3] >= data[i][0];
            gc.setFill(bull ? Color.web("#E8534A", 0.75) : Color.web("#4A9EFF", 0.75));
            double bh = (data[i][4] / maxVol) * (ch - 4);
            double x  = PAD_L + i * barW + barW * 0.20;
            gc.fillRect(x, ch - bh, Math.max(barW * 0.60, 2), bh);
        }

        // Y축 레이블
        gc.setFill(Color.web("#8899aa"));
        gc.setFont(Font.font("맑은 고딕", 10));
        gc.fillText(String.format("%,.0f", maxVol),     cw - AXIS_W + 4, 12);
        gc.fillText(String.format("%,.0f", maxVol / 2), cw - AXIS_W + 4, ch / 2 + 4);

        // ❺ 세로 커서 (캔들 영역과 동기화)
        if (mouseX >= 0 && mouseX <= cw - AXIS_W) {
            gc.setStroke(Color.web("#ffffff", 0.28));
            gc.setLineWidth(1);
            gc.setLineDashes(4, 4);
            gc.strokeLine(mouseX, 0, mouseX, ch);
            gc.setLineDashes();
        }
    }

    // ────────────────────────────────────────────────────────────
    //  ❷ 툴팁 & ❸ 현재가 레이블 갱신
    // ────────────────────────────────────────────────────────────
    private void updateTooltip(double mx, double my) {
        double cw   = candleCanvas.getWidth();
        double barW = (cw - AXIS_W - PAD_L - PAD_R) / data.length;
        int idx = (int)((mx - PAD_L) / barW);
        if (idx < 0 || idx >= data.length) {
            tooltipBox.setVisible(false);
            return;
        }

        double[] d = data[idx];

        // ❷ 툴팁 — 시가/고가/저가/종가/거래량 모두 표시
        tooltipBox.setText(String.format(
            "시가: %,.0f\n고가: %,.0f\n저가: %,.0f\n종가: %,.0f\n거래량: %,.0f",
            d[0], d[1], d[2], d[3], d[4]
        ));
        tooltipBox.setVisible(true);

        // 화면 밖 넘침 보정
        double tx = mx + 14, ty = my + 14;
        if (tx + 150 > cw - AXIS_W) tx = mx - 160;
        if (ty + 110 > candleH)     ty = my - 120;
        StackPane.setMargin(tooltipBox, new Insets(ty, 0, 0, tx));

        // ❸ 헤더 현재가 색상 + 값 갱신
        boolean bull = d[3] >= d[0];
        priceLabel.setText(String.format("%,.0f원", d[3]));
        priceLabel.setStyle(
            "-fx-text-fill: " + (bull ? "#ff6b6b" : "#4A9EFF") + ";" +
            "-fx-font-family: '맑은 고딕';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 24px;"
        );
    }
}