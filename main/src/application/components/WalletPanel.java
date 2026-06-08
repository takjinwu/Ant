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

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * WalletPanel
 * ─ 현재 보유 금액과 보유 주식 목록을 표시하는 패널
 * ─ 주가 변동 시 updatePrices() 를 호출하면 손익률이 실시간으로 갱신됨
 */
public class WalletPanel extends VBox {

    // ── 상수 ───────────────────────────────────────────────
    private static final NumberFormat MONEY_FMT =
            NumberFormat.getNumberInstance(Locale.KOREA);

    // ── 상태 ───────────────────────────────────────────────
    private long cash = 10_000_000L;                        // 초기 보유 현금 (1천만 원)
    private final Map<String, int[]> holdings =             // 종목명 → [수량, 평균단가]
            new LinkedHashMap<>();
    private final Map<String, Long> currentPrices =         // 종목명 → 현재가
            new HashMap<>();

    // ── UI 요소 ────────────────────────────────────────────
    private final Label cashValueLabel;
    private final VBox  stockListBox;

    // 보유 주식 클릭 콜백 (종목명 전달)
    private Consumer<String> onHoldingSelected = null;

    // ── 생성자 ─────────────────────────────────────────────
    public WalletPanel(double width, double height) {
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
        Label header = new Label("💼  내 지갑");
        header.setFont(Font.font("SUIT", FontWeight.EXTRA_BOLD, 17));
        header.setTextFill(Color.web("#FFFFFF"));

        // ── 현금 섹션 ──
        Label cashLabel = new Label("보유 현금");
        cashLabel.setFont(Font.font("SUIT", FontWeight.BOLD, 13));
        cashLabel.setTextFill(Color.web("#AAB4D4"));

        cashValueLabel = new Label(formatMoney(cash) + " 원");
        cashValueLabel.setFont(Font.font("SUIT", FontWeight.EXTRA_BOLD, 22));
        cashValueLabel.setTextFill(Color.web("#7EDDFF"));
        cashValueLabel.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(126,221,255,0.45), 10, 0.3, 0, 0);"
        );

        VBox cashBox = new VBox(4, cashLabel, cashValueLabel);
        cashBox.setPadding(new Insets(10, 14, 10, 14));
        cashBox.setStyle(
            "-fx-background-color: rgba(255,255,255,0.07);" +
            "-fx-background-radius: 14;"
        );

        // ── 보유 주식 섹션 ──
        Label stockHeader = new Label("보유 주식");
        stockHeader.setFont(Font.font("SUIT", FontWeight.BOLD, 13));
        stockHeader.setTextFill(Color.web("#AAB4D4"));

        stockListBox = new VBox(6);
        stockListBox.setPadding(new Insets(0));

        // 빈 안내 문구
        Label empty = new Label("보유 주식이 없습니다.");
        empty.setFont(Font.font("SUIT", 13));
        empty.setTextFill(Color.web("#667799"));
        stockListBox.getChildren().add(empty);

        ScrollPane scroll = new ScrollPane(stockListBox);
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

        getChildren().addAll(header, cashBox, stockHeader, scroll);
    }

    // ── 공개 API ────────────────────────────────────────────

    /** 현재 보유 현금 반환 */
    public long getCash() {
        return cash;
    }

    /** 결과창 띄우기 */
    public Map<String, int[]> getHoldings() {
        return new java.util.LinkedHashMap<>(holdings);
    }

    /**
     * 주가 변동 시 외부에서 호출 — 현재가를 갱신하고 손익률을 다시 그립니다.
     * @param prices 종목명 → 현재가 맵 (보유하지 않은 종목이 포함돼도 무방)
     */
    public void updatePrices(Map<String, Long> prices) {
        currentPrices.putAll(prices);
        refresh();
    }

    /**
     * 단일 종목 현재가 갱신 — 해당 종목만 업데이트하고 UI를 다시 그립니다.
     * @param stockName 종목명
     * @param price     현재가
     */
    public void updatePrice(String stockName, long price) {
        currentPrices.put(stockName, price);
        refresh();
    }

    /**
     * 주식 매수 처리
     * @param stockName     종목명
     * @param quantity      수량
     * @param pricePerShare 주당 가격
     * @return 매수 성공 여부
     */
    public boolean buy(String stockName, int quantity, long pricePerShare) {
        long totalCost = (long) quantity * pricePerShare;
        if (totalCost > cash || quantity <= 0) return false;

        cash -= totalCost;

        if (holdings.containsKey(stockName)) {
            int[] info     = holdings.get(stockName);
            long  prevTotal = (long) info[0] * info[1];
            info[0] += quantity;
            info[1] = (int) ((prevTotal + totalCost) / info[0]); // 평균단가 갱신
        } else {
            holdings.put(stockName, new int[]{quantity, (int) pricePerShare});
        }

        // 매수 시점 가격을 현재가로도 등록 (첫 매수라면)
        currentPrices.putIfAbsent(stockName, pricePerShare);

        refresh();
        return true;
    }

    /**
     * 주식 매도 처리
     * @param stockName     종목명
     * @param quantity      수량
     * @param pricePerShare 주당 가격
     * @return 매도 성공 여부
     */
    public boolean sell(String stockName, int quantity, long pricePerShare) {
        if (!holdings.containsKey(stockName)) return false;
        int[] info = holdings.get(stockName);
        if (info[0] < quantity || quantity <= 0) return false;

        cash += (long) quantity * pricePerShare;
        info[0] -= quantity;
        if (info[0] == 0) {
            holdings.remove(stockName);
            currentPrices.remove(stockName);
        }

        refresh();
        return true;
    }

    /** 보유 주식 행 클릭 시 호출될 리스너 등록 */
    public void setOnHoldingSelected(Consumer<String> listener) {
        this.onHoldingSelected = listener;
    }

    /**
     * 상장폐지 처리 — 보유 중인 해당 종목을 강제 제거합니다 (현금 환급 없음).
     * @return 소각된 평가금액 (보유수량 × 평균단가). 보유 없으면 0.
     */
    public long delistStock(String stockName) {
        if (!holdings.containsKey(stockName)) return 0L;
        int[] info = holdings.get(stockName);
        long lostValue = (long) info[0] * info[1];
        holdings.remove(stockName);
        currentPrices.remove(stockName);
        refresh();
        return lostValue;
    }

    /** 특정 종목 보유 수량 반환 (없으면 0) */
    public int getQuantity(String stockName) {
        int[] info = holdings.get(stockName);
        return info == null ? 0 : info[0];
    }

    // ── 내부 UI 갱신 ────────────────────────────────────────

    private void refresh() {
        cashValueLabel.setText(formatMoney(cash) + " 원");
        stockListBox.getChildren().clear();

        if (holdings.isEmpty()) {
            Label empty = new Label("보유 주식이 없습니다.");
            empty.setFont(Font.font("SUIT", 13));
            empty.setTextFill(Color.web("#667799"));
            stockListBox.getChildren().add(empty);
            return;
        }

        for (Map.Entry<String, int[]> entry : holdings.entrySet()) {
            stockListBox.getChildren().add(buildStockRow(entry.getKey(), entry.getValue()));
        }
    }

    private HBox buildStockRow(String name, int[] info) {
        int  qty       = info[0];
        long avgPrice  = info[1];
        long curPrice  = currentPrices.getOrDefault(name, (long) avgPrice); // 현재가 없으면 평균단가

        long   pnlAmount = (curPrice - avgPrice) * qty;   // 손익금액
        double pnlRate   = avgPrice == 0 ? 0.0
                         : (curPrice - avgPrice) * 100.0 / avgPrice; // 손익률(%)

        // ── 종목명 (왼쪽) ──
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("SUIT", FontWeight.BOLD, 13));
        nameLabel.setTextFill(Color.web("#E0E8FF"));

        // ── 수량 + 평균단가 (오른쪽 위) ──
        Label qtyLabel = new Label(qty + "주");
        qtyLabel.setFont(Font.font("SUIT", FontWeight.BOLD, 13));
        qtyLabel.setTextFill(Color.web("#FFD580"));

        Label avgLabel = new Label("평균 " + formatMoney(avgPrice) + "원");
        avgLabel.setFont(Font.font("SUIT", 12));
        avgLabel.setTextFill(Color.web("#8899BB"));

        // ── 손익 표시 ──
        String sign       = pnlAmount >= 0 ? "▲ +" : "▼ ";
        String pnlText    = sign + formatMoney(pnlAmount) + "원  "
                          + (pnlAmount >= 0 ? "+" : "") + String.format("%.2f", pnlRate) + "%";
        Label  pnlLabel   = new Label(pnlText);
        pnlLabel.setFont(Font.font("SUIT", FontWeight.EXTRA_BOLD, 12));

        String baseStyle;
        if (pnlAmount > 0) {
            pnlLabel.setTextFill(Color.web("#FF3333"));
            pnlLabel.setStyle(
                "-fx-effect: dropshadow(gaussian, rgba(255,60,60,0.75), 10, 0.4, 0, 0);"
            );
            baseStyle = "-fx-background-color: rgba(255,60,60,0.18);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(255,80,80,0.55);" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;";
        } else if (pnlAmount < 0) {
            pnlLabel.setTextFill(Color.web("#33AAFF"));
            pnlLabel.setStyle(
                "-fx-effect: dropshadow(gaussian, rgba(50,150,255,0.75), 10, 0.4, 0, 0);"
            );
            baseStyle = "-fx-background-color: rgba(50,130,255,0.18);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(60,140,255,0.55);" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;";
        } else {
            pnlLabel.setTextFill(Color.web("#AAAAAA"));
            pnlLabel.setStyle("");
            baseStyle = "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-background-radius: 12;" +
                        "-fx-cursor: hand;";
        }

        VBox rightBox = new VBox(2, qtyLabel, avgLabel, pnlLabel);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, nameLabel, spacer, rightBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));

        row.setStyle(baseStyle);

        row.setOnMouseEntered(e -> row.setStyle(
            "-fx-background-color: rgba(231,76,60,0.16);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(231,76,60,0.45);" +
            "-fx-border-radius: 12;" +
            "-fx-cursor: hand;"
        ));

        row.setOnMouseExited(e -> row.setStyle(baseStyle));

        row.setOnMouseClicked(e -> {
            if (onHoldingSelected != null) {
                onHoldingSelected.accept(name);
            }
        });

        return row;
    }

    private static String formatMoney(long value) {
        return MONEY_FMT.format(value);
    }
}