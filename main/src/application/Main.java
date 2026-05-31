package application;

import application.components.ChartPanel;
import application.components.NewsPanel;
import application.components.OrderPanel;
import application.components.StockListPanel;
import application.components.WalletPanel;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class Main extends Application {

	private static final double CHART_H = 620;
	private static final double BOTTOM_H = 260;
	private static final double GAP = 18;
	private static final double COLUMN_H = CHART_H + GAP + BOTTOM_H;

	private StackPane appRoot;
	private Pane zoomLayer;
	private StockListPanel stockListRef;   // ← 추가: start()에서 selectFirst() 호출용
	
	private String uiFontFamily = Font.getDefault().getFamily();

	@Override
	public void start(Stage primaryStage) {
		try {
			
			// ── Moneygraphy Rounded 폰트 로드 (Canvas 렌더링용) ──
			Font.loadFont(getClass().getResourceAsStream("fonts/Moneygraphy-Rounded.ttf"), 12);
			Font loadedFont = Font.loadFont(getClass().getResourceAsStream("fonts/Moneygraphy-Rounded.ttf"), 34);
			if (loadedFont != null) uiFontFamily = loadedFont.getFamily();

			BorderPane root = new BorderPane();
			root.setPadding(new Insets(18));
			root.setStyle(
				"-fx-background-color: linear-gradient(to right, " +
					"#06124A 0%, " +
					"#1A0B5E 45%, " +
					"#5B006E 75%, " +
					"#9B005C 100%);"
			);

			NewsPanel newsPanel = new NewsPanel(560, COLUMN_H);
			ChartPanel chartPanel = new ChartPanel(760, CHART_H);

			newsPanel.setNewsListener((title, content, effect, turn) -> {
				System.out.println("뉴스 변경");
				System.out.println("제목: " + title);
				System.out.println("내용: " + content);
				System.out.println("영향: " + effect);
				System.out.println("턴: " + turn);

				chartPanel.addCandle(effect);  // 턴마다 새 캔들 추가

				// 현재 선택 종목의 가격·점 색상을 실제 차트 데이터와 동기화
				String curName = chartPanel.getCurrentStockName();
				if (curName != null && !curName.isEmpty() && stockListRef != null) {
					long newPrice = chartPanel.getLastClosePrice();
					stockListRef.updatePrice(curName, newPrice);
				}
			});

			newsPanel.setImageClickHandler(imageView -> {
				showImageZoom(imageView);
			});

			root.setTop(buildTop(primaryStage, newsPanel));
			root.setCenter(buildCenter(newsPanel, chartPanel));

			zoomLayer = new Pane();
			zoomLayer.setPickOnBounds(false);

			appRoot = new StackPane(root, zoomLayer);

			Scene scene = new Scene(appRoot, 1920, 1080);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

			primaryStage.initStyle(StageStyle.UNDECORATED);
			primaryStage.setTitle("개미증권");
			primaryStage.setScene(scene);
			primaryStage.setMaximized(true);

			primaryStage.show();

			// ── [핵심 수정] 화면이 뜬 직후 첫 번째 종목(삼성전자)을 자동 선택해
			//    ChartPanel 초기 차트를 StockListPanel 추세 데이터와 일치시킴 ──
			Platform.runLater(() -> {
				if (stockListRef != null) stockListRef.selectFirst();
			});

			showStartNotice(newsPanel);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showStartNotice(NewsPanel newsPanel) {
		Label notice = new Label("10초 후 시작됩니다");
		Button skipButton = new Button("스킵");

		notice.setFont(Font.font(uiFontFamily, FontWeight.EXTRA_BOLD, 34));
		notice.setTextFill(Color.WHITE);
		notice.setStyle(
			"-fx-background-color: rgba(0, 0, 0, 0.58);" +
			"-fx-background-radius: 22;" +
			"-fx-padding: 20 42 20 42;"
		);

		skipButton.setStyle(
			"-fx-background-color: rgba(255,255,255,0.22);" +
			"-fx-text-fill: white;" +
			"-fx-font-size: 18px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 16;" +
			"-fx-padding: 10 24 10 24;"
		);

		VBox noticeBox = new VBox(12, notice, skipButton);
		noticeBox.setAlignment(Pos.CENTER);

		StackPane.setAlignment(noticeBox, Pos.TOP_CENTER);
		StackPane.setMargin(noticeBox, new Insets(30, 0, 0, 0));

		appRoot.getChildren().add(noticeBox);

		final int[] count = {10};

		Timeline countdown = new Timeline(
			new KeyFrame(Duration.seconds(1), e -> {
				count[0]--;

				if (count[0] > 0) {
					notice.setText(count[0] + "초 후 시작됩니다");
				} else {
					appRoot.getChildren().remove(noticeBox);
					newsPanel.startGame();
				}
			})
		);

		countdown.setCycleCount(10);

		skipButton.setOnAction(e -> {
			countdown.stop();
			appRoot.getChildren().remove(noticeBox);
			newsPanel.startGame();
		});

		countdown.play();
	}

	private void showImageZoom(ImageView sourceImageView) {
		if (sourceImageView.getImage() == null) {
			return;
		}

		Bounds sceneBounds = sourceImageView.localToScene(sourceImageView.getBoundsInLocal());

		ImageView zoomImage = new ImageView(sourceImageView.getImage());
		zoomImage.setPreserveRatio(false);
		zoomImage.setFitWidth(sceneBounds.getWidth());
		zoomImage.setFitHeight(sceneBounds.getHeight());
		zoomImage.setLayoutX(sceneBounds.getMinX());
		zoomImage.setLayoutY(sceneBounds.getMinY());
		zoomImage.setStyle(
			"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 45, 0.4, 0, 0);"
		);

		zoomLayer.setPickOnBounds(true);
		zoomLayer.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
		zoomLayer.getChildren().add(zoomImage);

		double targetW = 1100;
		double targetH = 650;
		double targetX = (appRoot.getWidth() - targetW) / 2.0;
		double targetY = (appRoot.getHeight() - targetH) / 2.0;

		Timeline open = new Timeline(
			new KeyFrame(Duration.millis(350),
				new KeyValue(zoomImage.layoutXProperty(), targetX, Interpolator.EASE_BOTH),
				new KeyValue(zoomImage.layoutYProperty(), targetY, Interpolator.EASE_BOTH),
				new KeyValue(zoomImage.fitWidthProperty(), targetW, Interpolator.EASE_BOTH),
				new KeyValue(zoomImage.fitHeightProperty(), targetH, Interpolator.EASE_BOTH)
			)
		);

		open.play();

		zoomImage.setOnMouseClicked(e -> {
			Timeline close = new Timeline(
				new KeyFrame(Duration.millis(350),
					new KeyValue(zoomImage.layoutXProperty(), sceneBounds.getMinX(), Interpolator.EASE_BOTH),
					new KeyValue(zoomImage.layoutYProperty(), sceneBounds.getMinY(), Interpolator.EASE_BOTH),
					new KeyValue(zoomImage.fitWidthProperty(), sceneBounds.getWidth(), Interpolator.EASE_BOTH),
					new KeyValue(zoomImage.fitHeightProperty(), sceneBounds.getHeight(), Interpolator.EASE_BOTH)
				)
			);

			close.setOnFinished(event -> {
				zoomLayer.getChildren().clear();
				zoomLayer.setStyle("");
				zoomLayer.setPickOnBounds(false);
			});

			close.play();
		});
	}

	private HBox buildTop(Stage stage, NewsPanel newsPanel) {
		javafx.scene.image.Image logoImg = new javafx.scene.image.Image(
				getClass().getResourceAsStream("images/logo.png"));
		javafx.scene.image.ImageView brand = new javafx.scene.image.ImageView(logoImg);
		brand.setPreserveRatio(true);
		brand.setFitHeight(56);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		Button fastForward = new Button("▶▶");
		fastForward.setStyle(
			"-fx-background-color: rgba(255,255,255,0.18);" +
			"-fx-text-fill: white;" +
			"-fx-font-size: 18px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 18;" +
			"-fx-padding: 10 22 10 22;" +
			"-fx-border-color: rgba(255,255,255,0.35);" +
			"-fx-border-radius: 18;"
		);
		fastForward.setOnAction(e -> newsPanel.nextTurn());

		Button exitButton = new Button("종료");
		exitButton.setStyle(
			"-fx-background-color: linear-gradient(to right, #ff416c, #ff4b2b);" +
			"-fx-text-fill: white;" +
			"-fx-font-size: 18px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 18;" +
			"-fx-padding: 10 26 10 26;" +
			"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 12, 0.3, 0, 3);"
		);

		exitButton.setOnMouseEntered(e -> {
			exitButton.setStyle(
				"-fx-background-color: linear-gradient(to right, #ff6a88, #ff7043);" +
				"-fx-text-fill: white;" +
				"-fx-font-size: 18px;" +
				"-fx-font-weight: bold;" +
				"-fx-background-radius: 18;" +
				"-fx-padding: 10 26 10 26;" +
				"-fx-effect: dropshadow(gaussian, rgba(255,75,43,0.55), 18, 0.45, 0, 4);"
			);
		});

		exitButton.setOnMouseExited(e -> {
			exitButton.setStyle(
				"-fx-background-color: linear-gradient(to right, #ff416c, #ff4b2b);" +
				"-fx-text-fill: white;" +
				"-fx-font-size: 18px;" +
				"-fx-font-weight: bold;" +
				"-fx-background-radius: 18;" +
				"-fx-padding: 10 26 10 26;" +
				"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 12, 0.3, 0, 3);"
			);
		});

		exitButton.setOnAction(e -> stage.close());

		HBox top = new HBox(12, brand, spacer, fastForward, exitButton);
		top.setPadding(new Insets(0, 0, 14, 0));
		top.setAlignment(Pos.CENTER_LEFT);

		return top;
	}

	private HBox buildCenter(NewsPanel newsPanel, ChartPanel chartPanel) {
		OrderPanel order = new OrderPanel(370, BOTTOM_H);
		WalletPanel wallet = new WalletPanel(370, BOTTOM_H);
		StockListPanel stockList = new StockListPanel(520, COLUMN_H);
		stockListRef = stockList;   // ← 추가: start()에서 접근하기 위해 저장

		// ── 패널 간 연동 ──
		order.setWallet(wallet);                          // 주문 → 지갑(체결/잔액)
		stockList.setChartPanel(chartPanel);              // 동그라미 색상이 실제 차트 데이터와 동기화
		stockList.setOnStockSelected((name, price) -> {   // 종목 선택 → 주문 패널 + 차트
			order.setSelectedStock(name, price);
			chartPanel.showStock(name, price);
		});

		wallet.setOnHoldingSelected(name -> {            // 보유 주식 선택 → 매도모드
			long price = stockList.getPrice(name);
			order.setSelectedHolding(name, price);
			chartPanel.showStock(name, price);
		});

		VBox leftColumn = buildLeftColumn(chartPanel, order, wallet);

		HBox center = new HBox(
				18,
				leftColumn,
				stockList,
				newsPanel
		);

		center.setAlignment(Pos.TOP_LEFT);

		return center;
	}

	private VBox buildLeftColumn(ChartPanel chart, OrderPanel order, WalletPanel wallet) {
		HBox bottom = new HBox(GAP, order, wallet);
		VBox left = new VBox(GAP, chart, bottom);

		left.setPrefHeight(COLUMN_H);

		return left;
	}

	public static void main(String[] args) {
		launch(args);
	}
}