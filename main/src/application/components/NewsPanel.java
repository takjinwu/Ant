package application.components;

import java.util.Random;
import java.util.function.Consumer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class NewsPanel extends Pane {

	private static final String FONT = "SUIT";

	private final javafx.scene.layout.StackPane card;
	private final Label timeLabel;
	private final Label turnLabel;
	private final Label titleLabel;
	private final Label contentLabel;
	private final ImageView newsImage;

	private int turn = 0;
	private int currentEffect = 0;
	private int elapsedSeconds = 0;

	private boolean gameStarted = false;

	private String currentNewsTitle = "";

	private NewsListener listener;
	private Consumer<ImageView> imageClickHandler;

	private final Random random = new Random();

	private Timeline clockTimer;
	private Timeline newsTimer;

	private final String[] normalNewsTitles = {
			"AI 반도체 시장 성장세",
			"전기차 배터리 수요 증가",
			"환율 안정화 기대감",
			"정부, 신산업 투자 확대 발표",
			"소비 심리 소폭 회복",
			"대형 기업 신제품 공개"
	};

	private final String[] normalNewsContents = {
			"AI 서버 수요 증가로 반도체 관련주에 관심이 모이고 있습니다.",
			"전기차 시장 확대 기대감으로 2차전지 업종이 주목받고 있습니다.",
			"환율 변동성이 줄어들며 수출 기업의 부담이 완화될 수 있습니다.",
			"정부 지원 기대감으로 신산업 관련 종목에 매수세가 유입될 수 있습니다.",
			"소비 심리가 회복되며 유통, 서비스 업종에 긍정적인 분위기가 형성되고 있습니다.",
			"신제품 출시 기대감으로 관련 기업의 주가 변동성이 확대되고 있습니다."
	};

	private final int[] normalEffects = {
			7,
			9,
			10,
			3,
			8,
			1
	};

	private final String[] crisisNewsTitles = {
			"글로벌 금융위기 발생",
			"주요국 증시 동반 폭락",
			"금융기관 유동성 위기 확산",
			"경기 침체 우려 급증"
	};

	private final String[] crisisNewsContents = {
			"글로벌 금융시장의 불안이 커지며 투자 심리가 급격히 위축되고 있습니다.",
			"주요국 증시가 일제히 하락하며 시장 전반에 강한 매도세가 나타나고 있습니다.",
			"금융기관의 유동성 문제가 확산되며 시장의 불확실성이 커지고 있습니다.",
			"경기 침체 가능성이 부각되며 위험자산 회피 심리가 강해지고 있습니다."
	};

	private final int[] crisisEffects = {
			-9,
			-8,
			-10,
			-7
	};

	// 나중에 사진 넣을 때 여기 경로만 바꾸면 됨
	private final String[] normalImagePaths = {
			"/Newsimg/p1.png",
			"/Newsimg/p1.png",
			"/Newsimg/p1.png",
			"/Newsimg/p1.png",
			"/Newsimg/p1.png",
			"/Newsimg/p1.png"
	};

	private final String[] crisisImagePaths = {
			"/Newsimg/p2.png",
			"/Newsimg/p3.png",
			"/Newsimg/p4.png",
			"/Newsimg/p5.png"
	};

	public NewsPanel(double width, double height) {
		setPrefSize(width, height);

		card = new javafx.scene.layout.StackPane();
		card.getStyleClass().add("panel");
		card.setPrefSize(width, height);
		card.setMaxHeight(height);

		timeLabel = new Label("00:00");
		timeLabel.setFont(Font.font(FONT, FontWeight.BOLD, 18));
		timeLabel.setTextFill(Color.WHITE);

		turnLabel = new Label("TURN 0");
		turnLabel.setFont(Font.font(FONT, FontWeight.BOLD, 18));
		turnLabel.setTextFill(Color.WHITE);

		newsImage = new ImageView();
		newsImage.setFitWidth(width - 60);
		newsImage.setFitHeight(260);
		newsImage.setPreserveRatio(false);
		newsImage.setStyle(
			"-fx-background-color: rgba(255,255,255,0.18);" +
			"-fx-border-color: rgba(255,255,255,0.35);" +
			"-fx-border-radius: 18;" +
			"-fx-background-radius: 18;"
		);

		titleLabel = new Label("뉴스 준비 중");
		titleLabel.setFont(Font.font(FONT, FontWeight.EXTRA_BOLD, 28));
		titleLabel.setTextFill(Color.WHITE);
		titleLabel.setWrapText(true);
		titleLabel.setPrefWidth(width - 60);

		contentLabel = new Label("상단 카운트다운이 끝나면 뉴스가 표시됩니다.");
		contentLabel.setFont(Font.font(FONT, FontWeight.NORMAL, 18));
		contentLabel.setTextFill(Color.web("#E8E8E8"));
		contentLabel.setWrapText(true);
		contentLabel.setPrefWidth(width - 60);

		getChildren().addAll(card, timeLabel, turnLabel, newsImage, titleLabel, contentLabel);

		card.setLayoutX(0);
		card.setLayoutY(0);

		timeLabel.setLayoutX(30);
		timeLabel.setLayoutY(72);

		turnLabel.setLayoutX(width - 130);
		turnLabel.setLayoutY(72);
		turnLabel.setAlignment(Pos.CENTER_RIGHT);

		newsImage.setLayoutX(30);
		newsImage.setLayoutY(120);

		titleLabel.setLayoutX(30);
		titleLabel.setLayoutY(410);

		contentLabel.setLayoutX(30);
		contentLabel.setLayoutY(465);

		newsImage.setOnMouseClicked(e -> {
			if (imageClickHandler != null && newsImage.getImage() != null) {
				imageClickHandler.accept(newsImage);
			}
		});
	}

	public void startGame() {
		if (gameStarted) {
			return;
		}

		gameStarted = true;
		elapsedSeconds = 0;
		timeLabel.setText("00:00");

		startClock();
		nextTurn();
		startNewsTimer();
	}

	private void startClock() {
		clockTimer = new Timeline(
				new KeyFrame(Duration.seconds(1), e -> {
					elapsedSeconds++;

					int minutes = elapsedSeconds / 60;
					int seconds = elapsedSeconds % 60;

					timeLabel.setText(String.format("%02d:%02d", minutes, seconds));
				})
		);

		clockTimer.setCycleCount(Timeline.INDEFINITE);
		clockTimer.play();
	}

	private void startNewsTimer() {
		newsTimer = new Timeline(
				new KeyFrame(Duration.minutes(3), e -> nextTurn())
		);

		newsTimer.setCycleCount(Timeline.INDEFINITE);
		newsTimer.play();
	}

	public void nextTurn() {
		if (!gameStarted) {
			return;
		}
		
		turn++;
		turnLabel.setText("TURN " + turn);
		
		boolean isCrisisTurn = turn % 3 == 0;

		if (isCrisisTurn) {
			int index = random.nextInt(crisisNewsTitles.length);

			currentNewsTitle = crisisNewsTitles[index];
			currentEffect = crisisEffects[index];

			titleLabel.setText(crisisNewsTitles[index]);
			contentLabel.setText(crisisNewsContents[index]);

			loadNewsImage(crisisImagePaths[index]);

			if (listener != null) {
				listener.onNewsChanged(
						currentNewsTitle,
						crisisNewsContents[index],
						currentEffect,
						turn
				);
			}
		} else {
			int index = random.nextInt(normalNewsTitles.length);

			currentNewsTitle = normalNewsTitles[index];
			currentEffect = normalEffects[index];

			titleLabel.setText(normalNewsTitles[index]);
			contentLabel.setText(normalNewsContents[index]);

			loadNewsImage(normalImagePaths[index]);

			if (listener != null) {
				listener.onNewsChanged(
						currentNewsTitle,
						normalNewsContents[index],
						currentEffect,
						turn
				);
			}
		}
	}

	private void loadNewsImage(String path) {
	    try {
	        Image image = new Image(
	            getClass().getResource(path).toExternalForm()
	        );

	        newsImage.setImage(image);

	    } catch (Exception e) {
	        newsImage.setImage(null);
	        System.out.println("이미지 경로 오류: " + path);
	        e.printStackTrace();
	    }
	}

	public void setNewsListener(NewsListener listener) {
		this.listener = listener;
	}

	public void setImageClickHandler(Consumer<ImageView> imageClickHandler) {
		this.imageClickHandler = imageClickHandler;
	}

	public int getTurn() {
		return turn;
	}

	public int getCurrentEffect() {
		return currentEffect;
	}

	public String getCurrentNewsTitle() {
		return currentNewsTitle;
	}

	public interface NewsListener {
		void onNewsChanged(String title, String content, int effect, int turn);
	}
}