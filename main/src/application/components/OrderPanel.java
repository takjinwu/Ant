package application.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * OrderPanel
 * ─ 선택된 주식의 매수 / 매도 주문을 입력하는 패널
 *
 * 연동 필요:
 *   setWallet(WalletPanel)         → 잔액/보유수량 조회 및 체결 처리
 *   setSelectedStock(name, price)  → 외부(StockListPanel 등)에서 종목 선택 시 호출
 *
 * 수정: setFont() + setStyle() 혼용 제거 → 모든 폰트를 CSS -fx-font-* 로 통일
 *       (포커스/클릭 시 문자 크기가 바뀌는 현상 수정)
 */
public class OrderPanel extends VBox {

    // ── 현재 선택 종목 ─────────────────────────────────────
    private String selectedStock = null;
    private long   currentPrice  = 0L;

    // ── 연동 지갑 ──────────────────────────────────────────
    private WalletPanel wallet = null;

    // ── 주문 모드 ──────────────────────────────────────────
    private boolean sellMode = false;

    // ── UI 요소 ────────────────────────────────────────────
    private final Label     selectedLabel;
    private final Label     priceLabel;
    private final Label     ratioHeader;
    private final TextField qtyField;
    private final Label     feedbackLabel;

    // ── 매수/매도 버튼 고정 크기 ───────────────────────────
    private static final double BTN_WIDTH  = 130;
    private static final double BTN_HEIGHT = 44;

    // ── 생성자 ─────────────────────────────────────────────
    public OrderPanel(double width, double height) {
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
        setSpacing(12);

        // ── 헤더 ──
        Label header = new Label("📋  주문");
        header.setStyle(
            "-fx-font-size: 17px;" +
            "-fx-font-weight: extra-bold;" +
            "-fx-text-fill: white;"
        );

        // ── 종목 정보 ──
        selectedLabel = new Label("종목을 선택하세요");
        selectedLabel.setStyle(
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #C8D8FF;"
        );

        priceLabel = new Label("현재가: ─");
        priceLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #8899BB;"
        );

        VBox infoBox = new VBox(3, selectedLabel, priceLabel);
        infoBox.setPadding(new Insets(10, 14, 10, 14));
        infoBox.setStyle(
            "-fx-background-color: rgba(255,255,255,0.07);" +
            "-fx-background-radius: 14;"
        );

        // ── 퀵 비율 버튼 ──
        ratioHeader = new Label("비율 매수");
        ratioHeader.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #AAB4D4;"
        );

        Button btn10  = buildRatioButton("10%",  0.10);
        Button btn25  = buildRatioButton("25%",  0.25);
        Button btn50  = buildRatioButton("50%",  0.50);
        Button btnAll = buildRatioButton("전부", 1.00);

        HBox ratioBox = new HBox(6, btn10, btn25, btn50, btnAll);
        ratioBox.setAlignment(Pos.CENTER_LEFT);

        // ── 수량 입력 ──
        Label qtyHeader = new Label("수량 (주)");
        qtyHeader.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #AAB4D4;"
        );

        qtyField = new TextField("0");
        // ★ setFont() 사용 안 함 — CSS 로만 폰트 지정해야 포커스 시 크기 불변
        qtyField.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #E8F0FF;" +
            "-fx-prompt-text-fill: #556688;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.22);" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 8 14 8 14;"
        );
        qtyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) qtyField.setText(newVal.replaceAll("[^\\d]", ""));
            clearFeedback();
        });

        // ── 매수 / 매도 버튼 ──
        Button buyButton  = buildOrderButton("매수", true);
        Button sellButton = buildOrderButton("매도", false);

        buyButton.setOnAction(e -> executeOrder(true));
        sellButton.setOnAction(e -> executeOrder(false));

        HBox orderBtnBox = new HBox(10, buyButton, sellButton);
        orderBtnBox.setAlignment(Pos.CENTER);
        VBox.setMargin(orderBtnBox, new Insets(10, 0, 0, 0));

        // ── 피드백 라벨 ──
        feedbackLabel = new Label("");
        feedbackLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #FFD580;"
        );
        feedbackLabel.setWrapText(true);

        getChildren().addAll(
            header,
            infoBox,
            ratioHeader, ratioBox,
            qtyHeader, qtyField,
            orderBtnBox,
            feedbackLabel
        );
    }

    // ── 공개 API ────────────────────────────────────────────

    public void setWallet(WalletPanel wallet) {
        this.wallet = wallet;
    }

    public void setSelectedStock(String stockName, long price) {
        this.selectedStock = stockName;
        this.currentPrice  = price;
        setBuyMode();
        selectedLabel.setText(stockName);
        priceLabel.setText("현재가: " + formatMoney(price) + " 원");
        qtyField.setText("0");
        clearFeedback();
    }

    public void setSelectedHolding(String stockName, long price) {
        this.selectedStock = stockName;
        this.currentPrice  = price;
        setSellMode();
        selectedLabel.setText(stockName + " (매도 선택)");
        priceLabel.setText("현재가: " + formatMoney(price) + " 원");
        qtyField.setText("0");
        clearFeedback();
    }

    public void updatePrice(long price) {
        this.currentPrice = price;
        priceLabel.setText("현재가: " + formatMoney(price) + " 원");
    }

    // ── 내부 로직 ────────────────────────────────────────────

    private void applyRatio(double ratio) {
        if (wallet == null || currentPrice <= 0) {
            setFeedback("⚠ 종목 또는 지갑이 연결되지 않았습니다.", false);
            return;
        }
        int maxQty;
        if (sellMode) {
            int heldQty = wallet.getQuantity(selectedStock);
            maxQty = (int) Math.floor(heldQty * ratio);
            if (ratio > 0 && maxQty == 0 && heldQty > 0) maxQty = 1;
        } else {
            long budget = (long) (wallet.getCash() * ratio);
            maxQty = (int) (budget / currentPrice);
        }
        qtyField.setText(String.valueOf(maxQty));
        clearFeedback();
    }

    private void executeOrder(boolean isBuy) {
        if (selectedStock == null || selectedStock.isBlank()) {
            setFeedback("⚠ 종목을 먼저 선택하세요.", false); return;
        }
        if (wallet == null) {
            setFeedback("⚠ 지갑이 연결되지 않았습니다.", false); return;
        }
        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
        } catch (NumberFormatException ex) {
            setFeedback("⚠ 올바른 수량을 입력하세요.", false); return;
        }
        if (qty <= 0) {
            setFeedback("⚠ 수량은 1 이상이어야 합니다.", false); return;
        }

        if (isBuy) {
            boolean ok = wallet.buy(selectedStock, qty, currentPrice);
            if (ok) {
                setFeedback("✅ " + selectedStock + " " + qty + "주 매수 완료!", true);
                showToast(true, selectedStock, qty);
            } else {
                setFeedback("❌ 잔액이 부족합니다.", false); return;
            }
        } else {
            boolean ok = wallet.sell(selectedStock, qty, currentPrice);
            if (ok) {
                setFeedback("✅ " + selectedStock + " " + qty + "주 매도 완료!", true);
                showToast(false, selectedStock, qty);
                setBuyMode();
                selectedLabel.setText("종목을 선택하세요");
                priceLabel.setText("현재가: ─");
                selectedStock = null;
                currentPrice = 0L;
            } else {
                int held = wallet.getQuantity(selectedStock);
                setFeedback("❌ 보유 수량 부족 (보유: " + held + "주)", false); return;
            }
        }
        qtyField.setText("0");
    }

    private void showToast(boolean isBuy, String stockName, int qty) {
        if (getScene() == null) return;
        if (!(getScene().getRoot() instanceof StackPane)) return;
        StackPane root = (StackPane) getScene().getRoot();

        String icon     = isBuy ? "🟢" : "🔴";
        String typeText = isBuy ? "매수" : "매도";
        String color    = isBuy ? "#1A3A2A" : "#3A1A1A";
        String border   = isBuy ? "#2ECC71" : "#E74C3C";
        String textCol  = isBuy ? "#7EDDAA" : "#FF9999";

        Label toastLabel = new Label(
            icon + "  " + stockName + "  " + qty + "주  " + typeText + " 주문이 체결됐습니다"
        );
        toastLabel.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + textCol + ";" +
            "-fx-background-color: " + color + ";" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.8;" +
            "-fx-padding: 14 32 14 32;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 22, 0.35, 0, 6);"
        );

        StackPane.setAlignment(toastLabel, Pos.TOP_CENTER);
        StackPane.setMargin(toastLabel, new Insets(30, 0, 0, 0));
        toastLabel.setTranslateY(-60);
        toastLabel.setOpacity(0);
        root.getChildren().add(toastLabel);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(280), toastLabel);
        slideIn.setFromY(-60);
        slideIn.setToY(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), toastLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.millis(2500));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), toastLabel);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> root.getChildren().remove(toastLabel));

        slideIn.play();
        fadeIn.play();
        slideIn.setOnFinished(e -> { pause.play(); pause.setOnFinished(ev -> fadeOut.play()); });
    }

    private void setBuyMode()  { sellMode = false; ratioHeader.setText("비율 매수"); }
    private void setSellMode() { sellMode = true;  ratioHeader.setText("비율 매도"); }

    private void setFeedback(String msg, boolean success) {
        feedbackLabel.setText(msg);
        // setText 후 style 재적용으로 색상 유지 (text-fill 만 바꾸고 나머지 보존)
        feedbackLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: " + (success ? "#7EDDFF" : "#FF7E7E") + ";"
        );
    }

    private void clearFeedback() {
        feedbackLabel.setText("");
        feedbackLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #FFD580;");
    }

    // ── 빌더 헬퍼 ───────────────────────────────────────────

    private Button buildRatioButton(String text, double ratio) {
        Button btn = new Button(text);
        btn.setPrefWidth(62);

        // ★ setFont() 없이 CSS 로만 폰트 지정
        String base = "-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-background-color: rgba(255,255,255,0.13);" +
            "-fx-text-fill: #D0E4FF;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: rgba(255,255,255,0.25);" +
            "-fx-border-radius: 10;" +
            "-fx-padding: 6 0 6 0;" +
            "-fx-cursor: hand;";
        String hover = "-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-background-color: rgba(126,221,255,0.22);" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: rgba(126,221,255,0.55);" +
            "-fx-border-radius: 10;" +
            "-fx-padding: 6 0 6 0;" +
            "-fx-cursor: hand;";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(e -> applyRatio(ratio));
        return btn;
    }

    private Button buildOrderButton(String text, boolean isBuy) {
        String baseColor  = isBuy
            ? "linear-gradient(to bottom, #2ECC71, #27AE60)"
            : "linear-gradient(to bottom, #E74C3C, #C0392B)";
        String hoverColor = isBuy
            ? "linear-gradient(to bottom, #58D68D, #2ECC71)"
            : "linear-gradient(to bottom, #F1948A, #E74C3C)";
        String shadow     = isBuy
            ? "dropshadow(gaussian, rgba(46,204,113,0.5), 14, 0.4, 0, 3)"
            : "dropshadow(gaussian, rgba(231,76,60,0.5), 14, 0.4, 0, 3)";

        // ★ 버튼도 -fx-font-size/-fx-font-weight 를 style 안에 포함
        String baseStyle = String.format(
            "-fx-background-color: %s;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 0;" +
            "-fx-effect: %s;" +
            "-fx-cursor: hand;",
            baseColor, shadow
        );
        String hoverStyle = String.format(
            "-fx-background-color: %s;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 0;" +
            "-fx-effect: %s;" +
            "-fx-cursor: hand;",
            hoverColor, shadow
        );

        Button btn = new Button(text);
        btn.setPrefWidth(BTN_WIDTH);
        btn.setPrefHeight(BTN_HEIGHT);
        btn.setMinWidth(BTN_WIDTH);
        btn.setMinHeight(BTN_HEIGHT);
        btn.setMaxWidth(BTN_WIDTH);
        btn.setMaxHeight(BTN_HEIGHT);
        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e  -> btn.setStyle(baseStyle));
        return btn;
    }

    private static String formatMoney(long value) {
        return String.format("%,d", value);
    }
}
