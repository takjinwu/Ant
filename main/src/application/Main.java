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

public class Main extends Application {

	private MediaPlayer bgmPlayer;

	private static final double BASE_W = 1920;
	private static final double BASE_H = 1080;

	private static final double CHART_H = 620;
	private static final double BOTTOM_H = 260;
	private static final double GAP = 18;
	private static final double COLUMN_H = CHART_H + GAP + BOTTOM_H;

	private StackPane appRoot;
	private Pane responsiveRoot;
	private Group scaledGroup;
	private Scale uiScale;
	private Pane zoomLayer;
	private StockListPanel stockListRef;
	private OrderPanel orderRef;

	private String uiFontFamily = Font.getDefault().getFamily();

	private Stage currentStage;
	private Scene mainScene;
	private WalletPanel walletRef;

	private enum DisplayMode {
		FULLSCREEN,
		BORDERLESS,
		WINDOWED
	}

	private DisplayMode currentMode = DisplayMode.BORDERLESS;

	@Override
	public void start(Stage primaryStage) {
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
					chartPanel.addCandleToAll(stockListRef.getStocks(), effect);
					stockListRef.updateAllPrices(chartPanel);
					// 턴 변경 후 OrderPanel에 선택된 종목의 현재가 갱신
					if (orderRef != null) {
						orderRef.refreshPrice(stockListRef);
					}
				} else {
					chartPanel.addCandle(effect);
				}
			});

			newsPanel.setImageClickHandler(imageView -> {
				showImageZoom(imageView);
			});

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

			showStartNotice(newsPanel);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void playBgm() {
		try {
			java.net.URL bgmUrl = getClass().getResource("/music/main_bgm.mp3");

			if (bgmUrl == null) {
				bgmUrl = getClass().getResource("music/main_bgm.mp3");
			}

			if (bgmUrl == null) {
				System.out.println("BGM 파일을 찾을 수 없습니다.");
				return;
			}

			Media bgm = new Media(bgmUrl.toExternalForm());
			bgmPlayer = new MediaPlayer(bgm);
			bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
			bgmPlayer.setVolume(0.1);
			bgmPlayer.play();

		} catch (Exception e) {
			e.printStackTrace();
		}
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

		// 오버레이 배경
		javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane(card);
		overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");

		appRoot.getChildren().add(overlay);

		closeBtn.setOnAction(e -> appRoot.getChildren().remove(overlay));
		// 오버레이 클릭 시 닫기
		overlay.setOnMouseClicked(e -> {
			if (e.getTarget() == overlay) appRoot.getChildren().remove(overlay);
		});
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
		javafx.scene.image.Image logoImg = new javafx.scene.image.Image(
			getClass().getResourceAsStream("images/logo.png")
		);

		javafx.scene.image.ImageView brand = new javafx.scene.image.ImageView(logoImg);
		brand.setPreserveRatio(true);
		brand.setFitHeight(56);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		Button fastForward = new Button("▶▶");
		fastForward.setStyle(topButtonStyle());
		fastForward.setOnAction(e -> newsPanel.nextTurn());

		Image gearImg = new Image(
			    getClass().getResourceAsStream("images/gear.png")
			);

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

		exitButton.setOnAction(e -> currentStage.close());

		HBox top = new HBox(12, brand, spacer, optionButton,fastForward,exitButton);
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

	    Slider volumeSlider = new Slider(0, 100, bgmPlayer != null ? bgmPlayer.getVolume() * 100 : 30);
	    volumeSlider.setPrefWidth(280);
	    volumeSlider.setShowTickLabels(true);
	    volumeSlider.setShowTickMarks(true);

	    volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
	        if (bgmPlayer != null) {
	            bgmPlayer.setVolume(newVal.doubleValue() / 100.0);
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

	    applyButton.setOnAction(e -> {
	        optionStage.close();

	        String mode = screenModeBox.getValue();

	        if (mode.equals("전체화면")) {
	        	applyDisplayMode(DisplayMode.FULLSCREEN);
	        }
	        else if (mode.equals("테두리 없음")) {
	        	applyDisplayMode(DisplayMode.BORDERLESS);
	        }
	        else if (mode.equals("창모드")) {
	        	applyDisplayMode(DisplayMode.WINDOWED);
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
	    Scene scene = new Scene(card, 420, 330);
	    scene.setFill(Color.TRANSPARENT);

	    optionStage.setScene(scene);
	    optionStage.show();
	    
	}
	private void updateResponsiveScale() {
		if (responsiveRoot == null || scaledGroup == null || uiScale == null) return;

		double w = responsiveRoot.getWidth();
		double h = responsiveRoot.getHeight();

		if (w <= 0 || h <= 0) return;

		// 1920x1080 기준으로 만든 UI를 현재 화면 안에 반드시 들어오도록 축소/확대한다.
		// 핵심: Scale의 pivot을 (0,0)으로 고정해야 아래로 밀리거나 클릭 위치가 어긋나지 않는다.
		double scale = Math.min(w / BASE_W, h / BASE_H);

		uiScale.setX(scale);
		uiScale.setY(scale);

		double scaledW = BASE_W * scale;
		double scaledH = BASE_H * scale;

		// 남는 공간은 중앙 정렬한다. 화면 비율이 달라도 UI가 잘리지 않는다.
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

		// ── 상장폐지 이벤트 처리 ──
		stockList.setOnDelisted(name -> {
			// ChartPanel에 상장폐지 마킹 (이후 캔들 추가 차단)
			chartPanel.delistStock(name);
			// 지갑에서 해당 종목 강제 소각 (현금 환급 없음)
			long lost = walletRef.delistStock(name);
			// 상장폐지 안내 팝업
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

	public static void main(String[] args) {
		launch(args);
	}
}