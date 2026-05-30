package application.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * StockListPanel
 * ─ 거래 가능한 종목 목록을 표시하는 패널
 * ─ 종목 행을 클릭하면 선택 리스너(setOnStockSelected)로 종목명/현재가를 전달
 *   → OrderPanel.setSelectedStock(name, price) 에 연결해서 사용
 * ─ WalletPanel / OrderPanel 과 동일한 글래스 카드 스타일
 */
public class StockListPanel extends VBox {

    // ── 종목명 → 현재가(원) ─────────────────────────────────
    // 이름은 정식 종목명으로 교정해서 등록 (한화에어로스페이스, LIG넥스원 등)
    private final Map<String, Long> stocks = new LinkedHashMap<>();

    // ── 선택 콜백 (종목명, 현재가) ───────────────────────────
    private BiConsumer<String, Long> onStockSelected = null;

    // ── UI 요소 ────────────────────────────────────────────
    private final VBox rowBox;
    private HBox selectedRow = null;

    // ── 생성자 ─────────────────────────────────────────────
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

        // ── 헤더 ──
        Label header = new Label("📈  주식 목록");
        header.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 17));
        header.setTextFill(Color.web("#FFFFFF"));

        // ── 컬럼 안내 ──
        Label nameCol = new Label("종목명");
        nameCol.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameCol.setTextFill(Color.web("#AAB4D4"));

        Region headSpacer = new Region();
        HBox.setHgrow(headSpacer, Priority.ALWAYS);

        Label priceCol = new Label("현재가");
        priceCol.setFont(Font.font("System", FontWeight.BOLD, 12));
        priceCol.setTextFill(Color.web("#AAB4D4"));

        HBox colHeader = new HBox(8, nameCol, headSpacer, priceCol);
        colHeader.setAlignment(Pos.CENTER_LEFT);
        colHeader.setPadding(new Insets(0, 12, 0, 12));

        // ── 종목 데이터 (정식 종목명 + 시작가) ──
        registerDefaultStocks();

        // ── 목록 영역 ──
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

    /** 거래 종목 12종 등록 — 이름은 정식 종목명으로 교정 */
    private void registerDefaultStocks() {
        stocks.put("삼성전자",          75_000L);
        stocks.put("SK하이닉스",        180_000L);
        stocks.put("셀트리온",          175_000L);
        stocks.put("유한양행",          120_000L);
        stocks.put("한화에어로스페이스", 320_000L);   // 입력: 한화에어로슾페이스
        stocks.put("LIG넥스원",         240_000L);   // 입력: LIG넥원
        stocks.put("LG에너지솔루션",     380_000L);
        stocks.put("에코프로비엠",       180_000L);
        stocks.put("네이버",            210_000L);
        stocks.put("카카오",            42_000L);
        stocks.put("현대차",            250_000L);
        stocks.put("기아",              105_000L);
    }

    // ── 공개 API ────────────────────────────────────────────

    /** 종목 행 클릭 시 호출될 리스너 등록 (예: OrderPanel::setSelectedStock) */
    public void setOnStockSelected(BiConsumer<String, Long> listener) {
        this.onStockSelected = listener;
    }

    /** 특정 종목 현재가 반환 (없으면 0) */
    public long getPrice(String stockName) {
        return stocks.getOrDefault(stockName, 0L);
    }

    /** 종목 현재가 갱신 + 화면 반영 */
    public void updatePrice(String stockName, long price) {
        if (!stocks.containsKey(stockName)) return;
        stocks.put(stockName, price);
        for (var node : rowBox.getChildren()) {
            if (node instanceof HBox row && stockName.equals(row.getUserData())) {
                Label priceLabel = (Label) ((VBox) row.getChildren().get(2)).getChildren().get(0);
                priceLabel.setText(formatMoney(price) + " 원");
            }
        }
    }

    // ── 내부 UI ─────────────────────────────────────────────

    private HBox buildStockRow(String name, long price) {
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#E0E8FF"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label priceLabel = new Label(formatMoney(price) + " 원");
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        priceLabel.setTextFill(Color.web("#7EDDFF"));

        VBox rightBox = new VBox(priceLabel);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(8, nameLabel, spacer, rightBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setUserData(name);
        applyRowStyle(row, false);

        row.setOnMouseEntered(e -> { if (row != selectedRow) applyRowStyle(row, true); });
        row.setOnMouseExited(e  -> { if (row != selectedRow) applyRowStyle(row, false); });
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

    private static String formatMoney(long value) {
        return String.format("%,d", value);
    }
}
