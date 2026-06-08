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
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.scene.input.KeyCode;
import javafx.scene.image.Image;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;


public class Main extends Application {

	private MediaPlayer bgmPlayer;

	private static final double BASE_W = 1920;
	private static final double BASE_H = 1080;
	private String currentMusic = "/music/calm.mp3";
	private String previousMusic = currentMusic;
	private static final double CHART_H = 620;
	private static final double BOTTOM_H = 260;
	private static final double GAP = 18;
	private static final double COLUMN_H = CHART_H + GAP + BOTTOM_H;
	private double currentVolume = 0.3;
	private StackPane appRoot;
	private Pane responsiveRoot;
	private Group scaledGroup;
	private Scale uiScale;
	private Pane zoomLayer;

	private StockListPanel stockListRef;
	private WalletPanel walletRef;
	private OrderPanel orderRef;

	private String uiFontFamily = Font.getDefault().getFamily();
	private boolean kaiserCrashStarted = false;
	private Stage currentStage;
	private Scene mainScene;

	private enum DisplayMode {
		FULLSCREEN,
		BORDERLESS,
		WINDOWED
	}

	private DisplayMode currentMode = DisplayMode.BORDERLESS;

	@Override
	public void start(Stage primaryStage) {
		kaiserCrashStarted = false;
		try {
			java.io.InputStream regularStream = getClass().getResourceAsStream("fonts/SUIT-Regular.ttf");
			java.io.InputStream boldStream = getClass().getResourceAsStream("fonts/SUIT-Bold.ttf");

			Font regFont = (regularStream != null) ? Font.loadFont(regularStream, 12) : null;
			Font loadedFont = (boldStream != null) ? Font.loadFont(boldStream, 34) : null;

			if (loadedFont != null) uiFontFamily = loadedFont.getFamily();
			else uiFontFamily = "SUIT";

			BorderPane root = new BorderPane();
			root.setPadding(new Insets(18));
			root.setStyle(
				"-fx-background-color: linear-gradient(to right, " +
					"#06124A 0%, " +
					"#1A0B5E 45%, " +
					"#5B006E 75%, " +
					"#9B005C 100%);"
			);

			playBgm();

			NewsPanel newsPanel = new NewsPanel(560, COLUMN_H);
			ChartPanel chartPanel = new ChartPanel(760, CHART_H);

			newsPanel.setNewsListener((title, content, effect, turn) -> {
				System.out.println("뉴스 변경");
				System.out.println("제목: " + title);
				System.out.println("내용: " + content);
				System.out.println("영향: " + effect);
				System.out.println("턴: " + turn);
				
				if (stockListRef != null) {
					// 뉴스가 영향을 주는 섹터별 효과를 종목별 효과로 변환한다.
					// 같은 뉴스라도 종목의 섹터에 따라 오르거나 내리거나 거의 안 움직인다.
					java.util.Map<String, Integer> sectorEffects = newsPanel.getCurrentSectorEffects();
					java.util.Map<String, Integer> effectByStock = new java.util.HashMap<>();
					for (String name : stockListRef.getStocks().keySet()) {
						String sector = StockListPanel.getSectorOf(name);
						int e = sectorEffects.getOrDefault(
								sector, sectorEffects.getOrDefault("전체", 0));
						effectByStock.put(name, e);
					}
					for (String name : stockListRef.getStocks().keySet()) {

					    if (name.equals("카이저 컴퍼니")) {
					        continue; // 카이저는 일반 뉴스 영향 안 받게 제외
					    }

					    String sector = StockListPanel.getSectorOf(name);

					    int e = sectorEffects.getOrDefault(
					            sector,
					            sectorEffects.getOrDefault("전체", 0));

					    effectByStock.put(name, e);
					}

					if (title.contains("카이저 컴퍼니 주가조작")) {
					    kaiserCrashStarted = true;
					}

					if (kaiserCrashStarted) {
					    effectByStock.put("카이저 컴퍼니", -300);
					} else {
					    effectByStock.put("카이저 컴퍼니", 150);
					}
					chartPanel.addCandleToAll(stockListRef.getStocks(), effectByStock);
					stockListRef.updateAllPrices(chartPanel);
					// 턴 변경 후 OrderPanel에 선택된 종목의 현재가 갱신
					if (orderRef != null) {
						orderRef.refreshPrice(stockListRef);
					}
					// 주가 변동 시 WalletPanel 손익률 갱신
					if (walletRef != null) {
						java.util.Map<String, Long> latestPrices = new java.util.HashMap<>();
						for (String name : stockListRef.getStocks().keySet()) {
							latestPrices.put(name, stockListRef.getPrice(name));
						}
						walletRef.updatePrices(latestPrices);
					}
				} else {
					chartPanel.addCandle(effect);
				}
			});

			newsPanel.setImageClickHandler(imageView -> showImageZoom(imageView));

			newsPanel.setGameFinishedHandler(() -> showGameOverDialog());

			currentStage = primaryStage;

			root.setTop(buildTop(newsPanel));
			root.setCenter(buildCenter(newsPanel, chartPanel));

			zoomLayer = new Pane();
			zoomLayer.setPickOnBounds(false);

			appRoot = new StackPane(root, zoomLayer);
			appRoot.setPrefSize(BASE_W, BASE_H);
			appRoot.setMinSize(BASE_W, BASE_H);
			appRoot.setMaxSize(BASE_W, BASE_H);

			root.setPrefSize(BASE_W, BASE_H);
			zoomLayer.setPrefSize(BASE_W, BASE_H);

			scaledGroup = new Group(appRoot);
			scaledGroup.setManaged(false);

			uiScale = new Scale(1, 1, 0, 0);
			scaledGroup.getTransforms().add(uiScale);

			responsiveRoot = new Pane(scaledGroup);
			responsiveRoot.setStyle(
				"-fx-background-color: linear-gradient(to right, " +
					"#06124A 0%, " +
					"#1A0B5E 45%, " +
					"#5B006E 75%, " +
					"#9B005C 100%);"
			);

			Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
			mainScene = new Scene(responsiveRoot, visualBounds.getWidth(), visualBounds.getHeight());
			mainScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

			responsiveRoot.widthProperty().addListener((obs, oldVal, newVal) -> updateResponsiveScale());
			responsiveRoot.heightProperty().addListener((obs, oldVal, newVal) -> updateResponsiveScale());

			mainScene.setOnKeyPressed(e -> {
				if (e.isAltDown() && e.getCode() == KeyCode.ENTER) {
					toggleDisplayMode();
				}
			});

			applyDisplayMode(DisplayMode.BORDERLESS);

			Platform.runLater(() -> {
				if (stockListRef != null) stockListRef.selectFirst();
			});

			showStartScreen(newsPanel);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void playBgm() {

		try {

			if(bgmPlayer != null) {
				bgmPlayer.stop();
			}

			java.net.URL bgmUrl = getClass().getResource(currentMusic);

			if (bgmUrl == null) {
				System.out.println("BGM 파일 없음 : " + currentMusic);
				return;
			}

			Media bgm = new Media(
				bgmUrl.toExternalForm()
			);

			bgmPlayer = new MediaPlayer(bgm);

			bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
			bgmPlayer.setVolume(currentVolume);

			bgmPlayer.play();

		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void showStartScreen(NewsPanel newsPanel) {
		Label title = new Label("개미증권");
		title.setFont(Font.font(uiFontFamily, FontWeight.EXTRA_BOLD, 58));
		title.setTextFill(Color.WHITE);

		Label desc = new Label(
			"뉴스를 확인하고 주식을 매수·매도하여\n" +
			"21턴 동안 최대한 높은 자산을 만들어보세요.\n\n" +
			"뉴스는 1분마다 자동으로 변경되며,\n" +
			"SKIP 버튼으로 바로 다음 턴으로 넘길 수 있습니다."
		);
		desc.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 22));
		desc.setTextFill(Color.web("#DDE8FF"));
		desc.setAlignment(Pos.CENTER);
		desc.setStyle("-fx-text-alignment: center;");
		desc.setWrapText(true);

		Button startBtn = new Button("게임 시작");
		applyHoverEffect(startBtn);
		startBtn.setStyle(
			"-fx-background-color: linear-gradient(to right, #00C6FF, #7F00FF);" +
			"-fx-text-fill: white;" +
			"-fx-font-size: 26px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 24;" +
			"-fx-padding: 16 70 16 70;" +
			"-fx-cursor: hand;" +
			"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 18, 0.35, 0, 4);"
		);

		VBox card = new VBox(30, title, desc, startBtn);
		card.setAlignment(Pos.CENTER);
		card.setPadding(new Insets(58, 90, 58, 90));
		card.setMaxWidth(760);
		card.setStyle(
			"-fx-background-color: linear-gradient(to bottom right," +
				"rgba(6,18,74,0.94), rgba(91,0,110,0.90), rgba(155,0,92,0.86));" +
			"-fx-background-radius: 38;" +
			"-fx-border-color: rgba(255,255,255,0.42);" +
			"-fx-border-radius: 38;" +
			"-fx-border-width: 1.5;" +
			"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.68), 55, 0.35, 0, 0);"
		);

		StackPane overlay = new StackPane(card);
		overlay.setStyle("-fx-background-color: rgba(0,0,0,0.58);");
		overlay.setPrefSize(BASE_W, BASE_H);

		appRoot.getChildren().add(overlay);

		startBtn.setOnAction(e -> {
			appRoot.getChildren().remove(overlay);
			newsPanel.startGame();
		});
	}

	private void showGameOverDialog() {
		Label icon = new Label("📈");
		icon.setStyle("-fx-font-size: 52px;");

		Label title = new Label("게임종료!");
		title.setFont(Font.font(uiFontFamily, FontWeight.EXTRA_BOLD, 50));
		title.setTextFill(Color.WHITE);

		Label desc = new Label("21턴이 모두 종료되었습니다.\n최종 결과를 확인하거나 다시 도전할 수 있습니다.");
		desc.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 21));
		desc.setTextFill(Color.web("#DDE8FF"));
		desc.setAlignment(Pos.CENTER);
		desc.setStyle("-fx-text-alignment: center;");
		desc.setWrapText(true);

		Button retryBtn = new Button("재도전");
		Button resultBtn = new Button("결과보기");
		applyHoverEffect(retryBtn);
		applyHoverEffect(resultBtn);
		String btnStyle =
			"-fx-background-color: rgba(255,255,255,0.22);" +
			"-fx-text-fill: white;" +
			"-fx-font-size: 21px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 18;" +
			"-fx-padding: 13 44 13 44;" +
			"-fx-border-color: rgba(255,255,255,0.38);" +
			"-fx-border-radius: 18;" +
			"-fx-cursor: hand;";

		retryBtn.setStyle(btnStyle);
		resultBtn.setStyle(btnStyle);

		HBox buttons = new HBox(20, retryBtn, resultBtn);
		buttons.setAlignment(Pos.CENTER);

		VBox card = new VBox(24, icon, title, desc, buttons);
		card.setAlignment(Pos.CENTER);
		card.setPadding(new Insets(52, 76, 52, 76));
		card.setMaxWidth(680);
		card.setStyle(
			"-fx-background-color: linear-gradient(to bottom right," +
				"rgba(6,18,74,0.95), rgba(91,0,110,0.92), rgba(155,0,92,0.88));" +
			"-fx-background-radius: 36;" +
			"-fx-border-color: rgba(255,255,255,0.42);" +
			"-fx-border-radius: 36;" +
			"-fx-border-width: 1.5;" +
			"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.70), 55, 0.35, 0, 0);"
		);

		StackPane overlay = new StackPane(card);
		overlay.setStyle("-fx-background-color: rgba(0,0,0,0.62);");
		overlay.setPrefSize(BASE_W, BASE_H);

		appRoot.getChildren().add(overlay);

		retryBtn.setOnAction(e -> {
			appRoot.getChildren().remove(overlay);
			restartGame();
		});

		resultBtn.setOnAction(e -> {
			appRoot.getChildren().remove(overlay);
			showFinalResultScreen();
		});
	}

	private void showFinalResultScreen() {
		Label title = new Label("🎮 게임 종료 - 최종 투자 결과");
		title.setFont(Font.font(uiFontFamily, FontWeight.EXTRA_BOLD, 46));
		title.setTextFill(Color.WHITE);

		// 1. 자산 및 수익률 계산
		long initialAsset = 10_000_000L; // 게임 시작 시 초기 자산 (1천만 원)
		long currentCash = walletRef.getCash();
		long totalStockValue = 0L;

		// WalletPanel로부터 현재 보유 주식 목록을 가져와 현재가 기준으로 총액 계산
		java.util.Map<String, int[]> currentHoldings = walletRef.getHoldings();
		for (java.util.Map.Entry<String, int[]> entry : currentHoldings.entrySet()) {
			String stockName = entry.getKey();
			int quantity = entry.getValue()[0];
			// 실시간 현재가를 가져오고, 없을 경우 평단가로 예외 처리
			long currentPrice = (stockListRef != null) ? stockListRef.getPrice(stockName) : entry.getValue()[1];
			totalStockValue += (long) quantity * currentPrice;
		}

		long finalTotalAsset = currentCash + totalStockValue;
		long profitOrLoss = finalTotalAsset - initialAsset;
		double profitRate = ((double) profitOrLoss / initialAsset) * 100.0;

		// 2. 총자산 정보 레이아웃 구성
		Label totalAssetLabel = new Label(String.format("최종 총 자산: %,d 원", finalTotalAsset));
		totalAssetLabel.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 26));
		totalAssetLabel.setTextFill(Color.web("#7EDDFF"));

		Label detailsLabel = new Label(String.format("(현금: %,d 원 | 주식 평가액: %,d 원)", currentCash, totalStockValue));
		detailsLabel.setFont(Font.font(uiFontFamily, FontWeight.NORMAL, 15));
		detailsLabel.setTextFill(Color.web("#AAB4D4"));

		// 3. 수익률 퍼센트 표시 (이득: 빨강 ▲ / 손해: 파랑 ▼)
		Label rateLabel = new Label();
		if (profitOrLoss > 0) {
			rateLabel.setText(String.format("수익률: ▲+%.2f%% (+%,d 원)", profitRate, profitOrLoss));
			rateLabel.setTextFill(Color.web("#FF5555")); // 상승 레드
		} else if (profitOrLoss < 0) {
			rateLabel.setText(String.format("수익률: ▼%.2f%% (%,d 원)", profitRate, profitOrLoss));
			rateLabel.setTextFill(Color.web("#5599FF")); // 하락 블루
		} else {
			rateLabel.setText("수익률: 0.00% (변동 없음)");
			rateLabel.setTextFill(Color.WHITE);
		}
		rateLabel.setFont(Font.font(uiFontFamily, FontWeight.EXTRA_BOLD, 28));

		VBox assetSummaryBox = new VBox(8, totalAssetLabel, detailsLabel, rateLabel);
		assetSummaryBox.setAlignment(Pos.CENTER);
		assetSummaryBox.setPadding(new Insets(15));
		assetSummaryBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 16;");

		// 4. 보유 주식 명세서 리스트 생성
		Label holdingTitle = new Label("📋 최종 보유 주식 내역");
		holdingTitle.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 18));
		holdingTitle.setTextFill(Color.web("#FFE5A3"));

		VBox holdingListContainer = new VBox(8);
		holdingListContainer.setPadding(new Insets(10));

		if (currentHoldings.isEmpty()) {
			Label noHolding = new Label("종료 시점에 보유한 주식이 없습니다.");
			noHolding.setFont(Font.font(uiFontFamily, 15));
			noHolding.setTextFill(Color.web("#8899BB"));
			holdingListContainer.getChildren().add(noHolding);
		} else {
			// 헤더 행 추가
			HBox rowHeader = new HBox(10);
			Label hName = new Label("종목명");   hName.setPrefWidth(140);  hName.setTextFill(Color.web("#AAB4D4"));
			Label hQty  = new Label("보유량");   hQty.setPrefWidth(80);    hQty.setTextFill(Color.web("#AAB4D4"));
			Label hAvg  = new Label("평균단가"); hAvg.setPrefWidth(110);   hAvg.setTextFill(Color.web("#AAB4D4"));
			Label hCur  = new Label("현재가");   hCur.setPrefWidth(110);   hCur.setTextFill(Color.web("#AAB4D4"));
			Label hEval = new Label("평가손익"); hEval.setPrefWidth(120);  hEval.setTextFill(Color.web("#AAB4D4"));
			rowHeader.getChildren().addAll(hName, hQty, hAvg, hCur, hEval);
			rowHeader.setStyle("-fx-border-color: rgba(255,255,255,0.15); -fx-border-width: 0 0 1 0; -fx-padding: 0 0 4 0;");
			holdingListContainer.getChildren().add(rowHeader);

			// 종목별 상세 정산 내역 출력
			for (java.util.Map.Entry<String, int[]> entry : currentHoldings.entrySet()) {
				String name = entry.getKey();
				int qty = entry.getValue()[0];
				long avgPrice = entry.getValue()[1];
				long curPrice = (stockListRef != null) ? stockListRef.getPrice(name) : avgPrice;
				
				long buyTotal = (long) qty * avgPrice;
				long evalTotal = (long) qty * curPrice;
				long stockProfit = evalTotal - buyTotal;
				double stockRate = buyTotal > 0 ? ((double) stockProfit / buyTotal) * 100.0 : 0.0;

				HBox row = new HBox(10);
				row.setAlignment(Pos.CENTER_LEFT);
				row.setPadding(new Insets(6, 4, 6, 4));
				row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8;");

				Label lName = new Label(name);       lName.setPrefWidth(140); lName.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 14)); lName.setTextFill(Color.WHITE);
				Label lQty  = new Label(qty + " 주"); lQty.setPrefWidth(80);  lQty.setTextFill(Color.web("#FFF0B3"));
				Label lAvg  = new Label(String.format("%,d원", avgPrice)); lAvg.setPrefWidth(110); lAvg.setTextFill(Color.web("#CCCCCC"));
				Label lCur  = new Label(String.format("%,d원", curPrice)); lCur.setPrefWidth(110); lCur.setTextFill(Color.web("#7EDDFF"));

				Label lEval = new Label();
				if (stockProfit > 0) {
					lEval.setText(String.format("+%.1f%%", stockRate));
					lEval.setTextFill(Color.web("#FF5555"));
				} else if (stockProfit < 0) {
					lEval.setText(String.format("%.1f%%", stockRate));
					lEval.setTextFill(Color.web("#5599FF"));
				} else {
					lEval.setText("0.0%");
					lEval.setTextFill(Color.WHITE);
				}
				lEval.setPrefWidth(120);
				lEval.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 14));

				row.getChildren().addAll(lName, lQty, lAvg, lCur, lEval);
				holdingListContainer.getChildren().add(row);
			}
		}

		// 종목이 많아질 것에 대비한 스크롤 패널 구성
		javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(holdingListContainer);
		scrollPane.setFitToWidth(true);
		scrollPane.setPrefHeight(200);
		scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
		scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12;");

		// 5. 레이아웃 합성 및 오버레이 스크린 생성
		VBox resultCard = new VBox(24, title, assetSummaryBox, holdingTitle, scrollPane);
		resultCard.setAlignment(Pos.CENTER);
		resultCard.setPadding(new Insets(45, 60, 45, 60));
		resultCard.setMaxWidth(800);
		resultCard.setStyle(
			"-fx-background-color: linear-gradient(to bottom right, rgba(10,20,70,0.95), rgba(70,0,90,0.95));" +
			"-fx-background-radius: 34;" +
			"-fx-border-color: rgba(255,255,255,0.35);" +
			"-fx-border-radius: 34;" +
			"-fx-border-width: 1.5;" +
			"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 55, 0.35, 0, 0);"
		);

		Button retryBtn = new Button("재도전");
		Button exitBtn = new Button("종료하기");

		applyHoverEffect(retryBtn);
		applyHoverEffect(exitBtn);

		String finalBtnStyle =
		    "-fx-background-color: rgba(255,255,255,0.22);" +
		    "-fx-text-fill: white;" +
		    "-fx-font-size: 22px;" +
		    "-fx-font-weight: bold;" +
		    "-fx-background-radius: 22;" +
		    "-fx-padding: 14 60 14 60;" +
		    "-fx-border-color: rgba(255,255,255,0.38);" +
		    "-fx-border-radius: 22;" +
		    "-fx-cursor: hand;";

		retryBtn.setStyle(finalBtnStyle);

		exitBtn.setStyle(
		    "-fx-background-color: linear-gradient(to right, #ff416c, #ff4b2b);" +
		    "-fx-text-fill: white;" +
		    "-fx-font-size: 22px;" +
		    "-fx-font-weight: bold;" +
		    "-fx-background-radius: 22;" +
		    "-fx-padding: 14 60 14 60;" +
		    "-fx-cursor: hand;" +
		    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 14, 0.3, 0, 3);"
		);

		HBox finalButtons = new HBox(20, retryBtn, exitBtn);
		finalButtons.setAlignment(Pos.CENTER);

		Region topSpace = new Region();
		Region bottomSpace = new Region();
		VBox.setVgrow(topSpace, Priority.ALWAYS);
		VBox.setVgrow(bottomSpace, Priority.ALWAYS);

		HBox bottomButtons = new HBox(18, retryBtn, exitBtn);
		bottomButtons.setAlignment(Pos.CENTER);

		VBox screen = new VBox(24, topSpace, resultCard, bottomSpace, bottomButtons);
		screen.setAlignment(Pos.CENTER);
		screen.setPadding(new Insets(60, 0, 60, 0));
		screen.setStyle(
			"-fx-background-color: linear-gradient(to bottom right, " +
				"#06124A 0%, #1A0B5E 45%, #5B006E 75%, #9B005C 100%);"
		);

		StackPane resultOverlay = new StackPane(screen);
		resultOverlay.setPrefSize(BASE_W, BASE_H);

		appRoot.getChildren().add(resultOverlay);
		retryBtn.setOnAction(e -> restartGame());
		exitBtn.setOnAction(e -> Platform.exit());
	}

	private void restartGame() {
		kaiserCrashStarted = false;
		if (bgmPlayer != null) {
			bgmPlayer.stop();
		}

		Stage oldStage = currentStage;
		Stage newStage = new Stage();

		if (oldStage != null) {
			oldStage.close();
		}

		try {
			start(newStage);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	private void ShowFinalResultScreen() {
		Label title = new Label("🎮 게임 종료 - 최종 투자 결과");
		title.setFont(Font.font(uiFontFamily, FontWeight.EXTRA_BOLD, 46));
		title.setTextFill(Color.WHITE);

		// 1. 자산 및 수익률 계산
		long initialAsset = 10_000_000L; // 게임 시작 시 초기 자산 (1천만 원)
		long currentCash = walletRef.getCash();
		long totalStockValue = 0L;

		// WalletPanel로부터 현재 보유 주식 목록을 가져와 현재가 기준으로 총액 계산
		java.util.Map<String, int[]> currentHoldings = walletRef.getHoldings();
		for (java.util.Map.Entry<String, int[]> entry : currentHoldings.entrySet()) {
			String stockName = entry.getKey();
			int quantity = entry.getValue()[0];
			// 실시간 현재가를 가져오고, 없을 경우 평단가로 예외 처리
			long currentPrice = (stockListRef != null) ? stockListRef.getPrice(stockName) : entry.getValue()[1];
			totalStockValue += (long) quantity * currentPrice;
		}

		long finalTotalAsset = currentCash + totalStockValue;
		long profitOrLoss = finalTotalAsset - initialAsset;
		double profitRate = ((double) profitOrLoss / initialAsset) * 100.0;

		// 2. 총자산 정보 레이아웃 구성
		Label totalAssetLabel = new Label(String.format("최종 총 자산: %,d 원", finalTotalAsset));
		totalAssetLabel.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 26));
		totalAssetLabel.setTextFill(Color.web("#7EDDFF"));

		Label detailsLabel = new Label(String.format("(현금: %,d 원 | 주식 평가액: %,d 원)", currentCash, totalStockValue));
		detailsLabel.setFont(Font.font(uiFontFamily, FontWeight.NORMAL, 15));
		detailsLabel.setTextFill(Color.web("#AAB4D4"));

		// 3. 수익률 퍼센트 표시 (이득: 빨강 ▲ / 손해: 파랑 ▼)
		Label rateLabel = new Label();
		if (profitOrLoss > 0) {
			rateLabel.setText(String.format("수익률: ▲+%.2f%% (+%,d 원)", profitRate, profitOrLoss));
			rateLabel.setTextFill(Color.web("#FF5555")); // 상승 레드
		} else if (profitOrLoss < 0) {
			rateLabel.setText(String.format("수익률: ▼%.2f%% (%,d 원)", profitRate, profitOrLoss));
			rateLabel.setTextFill(Color.web("#5599FF")); // 하락 블루
		} else {
			rateLabel.setText("수익률: 0.00% (변동 없음)");
			rateLabel.setTextFill(Color.WHITE);
		}
		rateLabel.setFont(Font.font(uiFontFamily, FontWeight.EXTRA_BOLD, 28));

		VBox assetSummaryBox = new VBox(8, totalAssetLabel, detailsLabel, rateLabel);
		assetSummaryBox.setAlignment(Pos.CENTER);
		assetSummaryBox.setPadding(new Insets(15));
		assetSummaryBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 16;");

		// 4. 보유 주식 명세서 리스트 생성
		Label holdingTitle = new Label("📋 최종 보유 주식 내역");
		holdingTitle.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 18));
		holdingTitle.setTextFill(Color.web("#FFE5A3"));

		VBox holdingListContainer = new VBox(8);
		holdingListContainer.setPadding(new Insets(10));

		if (currentHoldings.isEmpty()) {
			Label noHolding = new Label("종료 시점에 보유한 주식이 없습니다.");
			noHolding.setFont(Font.font(uiFontFamily, 15));
			noHolding.setTextFill(Color.web("#8899BB"));
			holdingListContainer.getChildren().add(noHolding);
		} else {
			// 헤더 행 추가
			HBox rowHeader = new HBox(10);
			Label hName = new Label("종목명");   hName.setPrefWidth(140);  hName.setTextFill(Color.web("#AAB4D4"));
			Label hQty  = new Label("보유량");   hQty.setPrefWidth(80);    hQty.setTextFill(Color.web("#AAB4D4"));
			Label hAvg  = new Label("평균단가"); hAvg.setPrefWidth(110);   hAvg.setTextFill(Color.web("#AAB4D4"));
			Label hCur  = new Label("현재가");   hCur.setPrefWidth(110);   hCur.setTextFill(Color.web("#AAB4D4"));
			Label hEval = new Label("평가손익"); hEval.setPrefWidth(120);  hEval.setTextFill(Color.web("#AAB4D4"));
			rowHeader.getChildren().addAll(hName, hQty, hAvg, hCur, hEval);
			rowHeader.setStyle("-fx-border-color: rgba(255,255,255,0.15); -fx-border-width: 0 0 1 0; -fx-padding: 0 0 4 0;");
			holdingListContainer.getChildren().add(rowHeader);

			// 종목별 상세 정산 내역 출력
			for (java.util.Map.Entry<String, int[]> entry : currentHoldings.entrySet()) {
				String name = entry.getKey();
				int qty = entry.getValue()[0];
				long avgPrice = entry.getValue()[1];
				long curPrice = (stockListRef != null) ? stockListRef.getPrice(name) : avgPrice;
				
				long buyTotal = (long) qty * avgPrice;
				long evalTotal = (long) qty * curPrice;
				long stockProfit = evalTotal - buyTotal;
				double stockRate = buyTotal > 0 ? ((double) stockProfit / buyTotal) * 100.0 : 0.0;

				HBox row = new HBox(10);
				row.setAlignment(Pos.CENTER_LEFT);
				row.setPadding(new Insets(6, 4, 6, 4));
				row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8;");

				Label lName = new Label(name);       lName.setPrefWidth(140); lName.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 14)); lName.setTextFill(Color.WHITE);
				Label lQty  = new Label(qty + " 주"); lQty.setPrefWidth(80);  lQty.setTextFill(Color.web("#FFF0B3"));
				Label lAvg  = new Label(String.format("%,d원", avgPrice)); lAvg.setPrefWidth(110); lAvg.setTextFill(Color.web("#CCCCCC"));
				Label lCur  = new Label(String.format("%,d원", curPrice)); lCur.setPrefWidth(110); lCur.setTextFill(Color.web("#7EDDFF"));

				Label lEval = new Label();
				if (stockProfit > 0) {
					lEval.setText(String.format("+%.1f%%", stockRate));
					lEval.setTextFill(Color.web("#FF5555"));
				} else if (stockProfit < 0) {
					lEval.setText(String.format("%.1f%%", stockRate));
					lEval.setTextFill(Color.web("#5599FF"));
				} else {
					lEval.setText("0.0%");
					lEval.setTextFill(Color.WHITE);
				}
				lEval.setPrefWidth(120);
				lEval.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 14));

				row.getChildren().addAll(lName, lQty, lAvg, lCur, lEval);
				holdingListContainer.getChildren().add(row);
			}
		}

		// 종목이 많아질 것에 대비한 스크롤 패널 구성
		javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(holdingListContainer);
		scrollPane.setFitToWidth(true);
		scrollPane.setPrefHeight(200);
		scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
		scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12;");

		// 5. 레이아웃 합성 및 오버레이 스크린 생성
		VBox resultCard = new VBox(24, title, assetSummaryBox, holdingTitle, scrollPane);
		resultCard.setAlignment(Pos.CENTER);
		resultCard.setPadding(new Insets(45, 60, 45, 60));
		resultCard.setMaxWidth(800);
		resultCard.setStyle(
			"-fx-background-color: linear-gradient(to bottom right, rgba(10,20,70,0.95), rgba(70,0,90,0.95));" +
			"-fx-background-radius: 34;" +
			"-fx-border-color: rgba(255,255,255,0.35);" +
			"-fx-border-radius: 34;" +
			"-fx-border-width: 1.5;" +
			"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 55, 0.35, 0, 0);"
		);

		Button exitBtn = new Button("게임 종료");
		applyHoverEffect(exitBtn);
		exitBtn.setStyle(
			"-fx-background-color: linear-gradient(to right, #ff416c, #ff4b2b);" +
			"-fx-text-fill: white;" +
			"-fx-font-size: 22px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 22;" +
			"-fx-padding: 14 80 14 80;" +
			"-fx-cursor: hand;" +
			"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 14, 0.3, 0, 3);"
		);

		Region topSpace = new Region();
		Region bottomSpace = new Region();
		VBox.setVgrow(topSpace, Priority.ALWAYS);
		VBox.setVgrow(bottomSpace, Priority.ALWAYS);

		VBox screen = new VBox(24, topSpace, resultCard, bottomSpace, exitBtn);
		screen.setAlignment(Pos.CENTER);
		screen.setPadding(new Insets(60, 0, 60, 0));
		screen.setStyle(
			"-fx-background-color: linear-gradient(to bottom right, " +
				"#06124A 0%, #1A0B5E 45%, #5B006E 75%, #9B005C 100%);"
		);

		StackPane resultOverlay = new StackPane(screen);
		resultOverlay.setPrefSize(BASE_W, BASE_H);

		appRoot.getChildren().add(resultOverlay);

		exitBtn.setOnAction(e -> Platform.exit());
	}

	private void showDelistNotice(String stockName, long lostValue) {
		Label icon  = new Label("📉");
		icon.setStyle("-fx-font-size: 42px;");

		Label title = new Label("상장폐지");
		title.setFont(Font.font(uiFontFamily, FontWeight.EXTRA_BOLD, 28));
		title.setTextFill(Color.web("#FF5555"));

		Label nameLabel = new Label(stockName);
		nameLabel.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 20));
		nameLabel.setTextFill(Color.WHITE);

		Label desc = new Label("주가가 1,000원 미만으로 하락하여\n상장 요건을 충족하지 못했습니다.");
		desc.setFont(Font.font(uiFontFamily, 14));
		desc.setTextFill(Color.web("#BBCCDD"));
		desc.setStyle("-fx-text-alignment: center;");
		desc.setWrapText(true);

		Label lossLabel;
		if (lostValue > 0) {
			lossLabel = new Label(String.format("보유 주식 손실: -%,d 원", lostValue));
			lossLabel.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 15));
			lossLabel.setTextFill(Color.web("#FF7777"));
			lossLabel.setStyle(
				"-fx-background-color: rgba(255,50,50,0.18);" +
				"-fx-background-radius: 10;" +
				"-fx-padding: 8 18 8 18;"
			);
		} else {
			lossLabel = new Label("보유 주식 없음 — 직접 손실 없음");
			lossLabel.setFont(Font.font(uiFontFamily, 14));
			lossLabel.setTextFill(Color.web("#88AACC"));
		}

		Button closeBtn = new Button("확인");
		applyHoverEffect(closeBtn);
		closeBtn.setStyle(
			"-fx-background-color: rgba(255,85,85,0.75);" +
			"-fx-text-fill: white;" +
			"-fx-font-family: '" + uiFontFamily + "';" +
			"-fx-font-size: 16px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 16;" +
			"-fx-padding: 10 36 10 36;" +
			"-fx-cursor: hand;"
		);

		VBox card = new VBox(14, icon, title, nameLabel, desc, lossLabel, closeBtn);
		card.setAlignment(Pos.CENTER);
		card.setPadding(new Insets(36, 48, 36, 48));
		card.setMaxWidth(420);
		card.setStyle(
			"-fx-background-color: linear-gradient(to bottom right," +
				"rgba(80,10,10,0.92), rgba(30,10,50,0.95));" +
			"-fx-background-radius: 28;" +
			"-fx-border-color: rgba(255,80,80,0.55);" +
			"-fx-border-radius: 28;" +
			"-fx-border-width: 1.5;" +
			"-fx-effect: dropshadow(gaussian, rgba(255,50,50,0.45), 40, 0.3, 0, 0);"
		);

		StackPane overlay = new StackPane(card);
		overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");

		appRoot.getChildren().add(overlay);

		closeBtn.setOnAction(e -> appRoot.getChildren().remove(overlay));
		overlay.setOnMouseClicked(e -> {
			if (e.getTarget() == overlay) appRoot.getChildren().remove(overlay);
		});
	}

	private void showImageZoom(ImageView sourceImageView) {
		if (sourceImageView.getImage() == null) return;

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

	private HBox buildTop(NewsPanel newsPanel) {
		Image logoImg = new Image(getClass().getResourceAsStream("images/logo.png"));
		ImageView brand = new ImageView(logoImg);
		brand.setPreserveRatio(true);
		brand.setFitHeight(56);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		Button fastForward = new Button("SKIP ▶▶");
		fastForward.setStyle(topButtonStyle());
		fastForward.setOnAction(e -> newsPanel.nextTurn());

		Image gearImg = new Image(getClass().getResourceAsStream("images/gear.png"));
		ImageView gearView = new ImageView(gearImg);
		gearView.setFitWidth(28);
		gearView.setFitHeight(28);

		Button optionButton = new Button();
		optionButton.setGraphic(gearView);
		optionButton.setStyle(
			"-fx-background-color: rgba(255,255,255,0.15);" +
			"-fx-background-radius: 50;" +
			"-fx-min-width: 50;" +
			"-fx-min-height: 50;" +
			"-fx-max-width: 50;" +
			"-fx-max-height: 50;" +
			"-fx-cursor: hand;"
		);

		optionButton.setOnAction(e -> showOptionWindow());

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
		applyHoverEffect(exitButton);
		applyHoverEffect(optionButton);
		applyHoverEffect(fastForward);
		exitButton.setOnAction(e -> Platform.exit());

		HBox top = new HBox(12, brand, spacer, optionButton, fastForward, exitButton);
		top.setPadding(new Insets(0, 0, 14, 0));
		top.setAlignment(Pos.CENTER_LEFT);

		return top;
	}

	private String topButtonStyle() {
		return "-fx-background-color: rgba(255,255,255,0.18);" +
			"-fx-text-fill: white;" +
			"-fx-font-size: 18px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 18;" +
			"-fx-padding: 10 22 10 22;" +
			"-fx-border-color: rgba(255,255,255,0.35);" +
			"-fx-border-radius: 18;";
	}

	private void showOptionWindow() {
		Stage stage = currentStage;
		final double[] xOffset = {0};
		final double[] yOffset = {0};

		Stage optionStage = new Stage();
		optionStage.initOwner(stage);
		optionStage.initModality(Modality.APPLICATION_MODAL);
		optionStage.initStyle(StageStyle.TRANSPARENT);

		Label title = new Label("설정");
		title.setFont(Font.font(uiFontFamily, FontWeight.EXTRA_BOLD, 30));
		title.setTextFill(Color.WHITE);

		Label volumeLabel = new Label("배경음악 볼륨");
		volumeLabel.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 16));
		volumeLabel.setTextFill(Color.WHITE);

		Slider volumeSlider = new Slider(
				0,
				100,
				currentVolume * 100
			);
		volumeSlider.setPrefWidth(280);
		volumeSlider.setShowTickLabels(true);
		volumeSlider.setShowTickMarks(true);

		volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {

			currentVolume = newVal.doubleValue() / 100.0;

			if (bgmPlayer != null) {
				bgmPlayer.setVolume(currentVolume);
			}
		});
		Label screenModeLabel = new Label("화면 모드");
		screenModeLabel.setFont(Font.font(uiFontFamily, FontWeight.BOLD, 16));
		screenModeLabel.setTextFill(Color.WHITE);

		ComboBox<String> screenModeBox = new ComboBox<>();
		screenModeBox.getItems().addAll("전체화면", "테두리 없음", "창모드");
		screenModeBox.setPrefWidth(280);

		if (currentMode == DisplayMode.FULLSCREEN) {
			screenModeBox.setValue("전체화면");
		} else if (currentMode == DisplayMode.BORDERLESS) {
			screenModeBox.setValue("테두리 없음");
		} else {
			screenModeBox.setValue("창모드");
		}

		Button applyButton = new Button("적용");
		Button closeButton = new Button("닫기");
		applyHoverEffect(applyButton);
		applyHoverEffect(closeButton);

		String optionBtnStyle =
			"-fx-background-color: rgba(255,255,255,0.20);" +
			"-fx-text-fill: white;" +
			"-fx-font-size: 16px;" +
			"-fx-font-weight: bold;" +
			"-fx-background-radius: 16;" +
			"-fx-padding: 10 30 10 30;" +
			"-fx-border-color: rgba(255,255,255,0.35);" +
			"-fx-border-radius: 16;";

		applyButton.setStyle(optionBtnStyle);
		closeButton.setStyle(optionBtnStyle);

		Label musicLabel = new Label("배경음악");

		musicLabel.setFont(
			Font.font(
				uiFontFamily,
				FontWeight.BOLD,
				16
			)
		);

		musicLabel.setTextFill(Color.WHITE);

		ComboBox<String> musicBox = new ComboBox<>();

		musicBox.getItems().addAll(
			"connect sky-2",
			"connect sky",
			"브금1",
			"브금2",
			"브금3",
			"브금4",
			"Underwater city"
		);

		if (currentMusic.equals("/music/main_bgm.mp3")) {
			musicBox.setValue("connect sky");
		}
		else if (currentMusic.equals("/music/calm.mp3")) {
			musicBox.setValue("connect sky-2");
		}
		else if (currentMusic.equals("/music/bull.mp3")) {
			musicBox.setValue("브금1");
		}
		else if (currentMusic.equals("/music/B2.mp3")) {
			musicBox.setValue("브금2");
		}
		else if (currentMusic.equals("/music/B3.mp3")) {
			musicBox.setValue("브금3");
		}
		else if (currentMusic.equals("/music/B4.mp3")) {
			musicBox.setValue("브금4");
		}
		else if (currentMusic.equals("/music/B5.mp3")) {
			musicBox.setValue("Underwater city");
		}
		else {
			musicBox.setValue("connect sky-2");
		}
		musicBox.setPrefWidth(280);
		applyButton.setOnAction(e -> {
			optionStage.close();
		

			String music = musicBox.getValue();

			String oldMusic = currentMusic;

			switch (music) {

				case "connect sky":
					currentMusic = "/music/main_bgm.mp3";
					break;

				case "connect sky-2":
					currentMusic = "/music/calm.mp3";
					break;

				case "브금1":
					currentMusic = "/music/bull.mp3";
					break;

				case "브금2":
					currentMusic = "/music/B2.mp3";
					break;

				case "브금3":
					currentMusic = "/music/B3.mp3";
					break;
					
				case "브금4":
					currentMusic = "/music/B4.mp3";
					break;
					
				case "Underwater city":
					currentMusic = "/music/B5.mp3";
					break;
					
			}

			if (!oldMusic.equals(currentMusic)) {
				playBgm();
			}
			
			String mode = screenModeBox.getValue();

			DisplayMode newMode;

			if (mode.equals("전체화면")) {
			    newMode = DisplayMode.FULLSCREEN;
			}
			else if (mode.equals("테두리 없음")) {
			    newMode = DisplayMode.BORDERLESS;
			}
			else {
			    newMode = DisplayMode.WINDOWED;
			}

			if (newMode != currentMode) {
			    applyDisplayMode(newMode);
			}
		});
	
		closeButton.setOnAction(e -> optionStage.close());

		HBox buttons = new HBox(12, applyButton, closeButton);
		buttons.setAlignment(Pos.CENTER);

		VBox card = new VBox(
			22,
			title,
			volumeLabel,
			volumeSlider,
			musicLabel,
			musicBox,
			screenModeLabel,
			screenModeBox,
			buttons
		);

		card.setAlignment(Pos.CENTER);
		card.setPadding(new Insets(34));

		card.setStyle(
			"-fx-background-color: linear-gradient(to bottom right, " +
				"rgba(6,18,74,0.78), " +
				"rgba(91,0,110,0.72), " +
				"rgba(155,0,92,0.68));" +
			"-fx-background-radius: 30;" +
			"-fx-border-color: rgba(255,255,255,0.32);" +
			"-fx-border-radius: 30;" +
			"-fx-border-width: 1.5;"
		);

		card.setOnMousePressed(e -> {
			xOffset[0] = e.getSceneX();
			yOffset[0] = e.getSceneY();
		});

		card.setOnMouseDragged(e -> {
			optionStage.setX(e.getScreenX() - xOffset[0]);
			optionStage.setY(e.getScreenY() - yOffset[0]);
		});
		


		Scene scene = new Scene(card, 420, 400);
		scene.setFill(Color.TRANSPARENT);

		optionStage.setScene(scene);
		optionStage.show();
	}

	private void updateResponsiveScale() {
		if (responsiveRoot == null || scaledGroup == null || uiScale == null) return;

		double w = responsiveRoot.getWidth();
		double h = responsiveRoot.getHeight();

		if (w <= 0 || h <= 0) return;

		double scale = Math.min(w / BASE_W, h / BASE_H);

		uiScale.setX(scale);
		uiScale.setY(scale);

		double scaledW = BASE_W * scale;
		double scaledH = BASE_H * scale;

		scaledGroup.setLayoutX(Math.max(0, (w - scaledW) / 2.0));
		scaledGroup.setLayoutY(Math.max(0, (h - scaledH) / 2.0));
	}

	private void toggleDisplayMode() {
		if (currentMode == DisplayMode.WINDOWED) {
			applyDisplayMode(DisplayMode.BORDERLESS);
		} else {
			applyDisplayMode(DisplayMode.WINDOWED);
		}
	}

	private void applyDisplayMode(DisplayMode mode) {
		Stage oldStage = currentStage;

		Screen targetScreen = getCurrentScreen(oldStage);
		Rectangle2D bounds = targetScreen.getBounds();
		Rectangle2D visualBounds = targetScreen.getVisualBounds();

		Stage newStage = new Stage();

		if (mode == DisplayMode.BORDERLESS) {
			newStage.initStyle(StageStyle.UNDECORATED);
		} else {
			newStage.initStyle(StageStyle.DECORATED);
		}

		newStage.setTitle("개미증권");
		newStage.setFullScreenExitHint("");

		if (oldStage != null) {
			oldStage.setScene(null);
			oldStage.close();
		}

		newStage.setScene(mainScene);
		currentStage = newStage;
		currentMode = mode;

		if (mode == DisplayMode.FULLSCREEN) {
			newStage.setX(bounds.getMinX());
			newStage.setY(bounds.getMinY());
			newStage.setWidth(bounds.getWidth());
			newStage.setHeight(bounds.getHeight());
			newStage.show();
			newStage.setFullScreen(true);
			Platform.runLater(() -> updateResponsiveScale());
		}
		else if (mode == DisplayMode.BORDERLESS) {
			newStage.setX(bounds.getMinX());
			newStage.setY(bounds.getMinY());
			newStage.setWidth(bounds.getWidth());
			newStage.setHeight(bounds.getHeight());
			newStage.show();
			Platform.runLater(() -> updateResponsiveScale());
		}
		else if (mode == DisplayMode.WINDOWED) {
			double winW = Math.min(1280, visualBounds.getWidth() * 0.9);
			double winH = Math.min(720, visualBounds.getHeight() * 0.9);

			newStage.setWidth(winW);
			newStage.setHeight(winH);

			double x = visualBounds.getMinX() + (visualBounds.getWidth() - winW) / 2;
			double y = visualBounds.getMinY() + (visualBounds.getHeight() - winH) / 2;

			newStage.setX(x);
			newStage.setY(y);

			newStage.show();
			newStage.setAlwaysOnTop(true);

			Platform.runLater(() -> {
				newStage.setAlwaysOnTop(false);
				updateResponsiveScale();
			});
		}
	}

	private Screen getCurrentScreen(Stage stage) {
		if (stage == null) {
			return Screen.getPrimary();
		}

		double centerX = stage.getX() + stage.getWidth() / 2;
		double centerY = stage.getY() + stage.getHeight() / 2;

		for (Screen screen : Screen.getScreens()) {
			Rectangle2D bounds = screen.getBounds();

			if (centerX >= bounds.getMinX() &&
				centerX <= bounds.getMaxX() &&
				centerY >= bounds.getMinY() &&
				centerY <= bounds.getMaxY()) {
				return screen;
			}
		}

		return Screen.getPrimary();
	}

	private HBox buildCenter(NewsPanel newsPanel, ChartPanel chartPanel) {
		OrderPanel order = new OrderPanel(370, BOTTOM_H);
		orderRef = order;
		WalletPanel wallet = new WalletPanel(370, BOTTOM_H);
		StockListPanel stockList = new StockListPanel(520, COLUMN_H);

		stockListRef = stockList;
		walletRef = wallet;

		order.setWallet(wallet);
		stockList.setChartPanel(chartPanel);

		stockList.setOnDelisted(name -> {
			chartPanel.delistStock(name);
			long lost = walletRef.delistStock(name);
			showDelistNotice(name, lost);
		});

		stockList.setOnStockSelected((name, price) -> {
			order.setSelectedStock(name, price);
			order.updatePrice(price);
			chartPanel.showStock(name, price);
		});

		wallet.setOnHoldingSelected(name -> {
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
	private void applyHoverEffect(Button button) {

		button.setOnMouseEntered(e -> {

			button.setScaleX(1.04);
			button.setScaleY(1.04);

			button.setEffect(
				new DropShadow(
					18,
					Color.rgb(255, 255, 255, 0.25)
				)
			);
		});

		button.setOnMouseExited(e -> {

			button.setScaleX(1.0);
			button.setScaleY(1.0);

			button.setEffect(null);
		});
	}
	public static void main(String[] args) {
		launch(args);
	}
}