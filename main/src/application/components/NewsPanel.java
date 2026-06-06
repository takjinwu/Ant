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
import java.util.ArrayList;
import java.util.List;


public class NewsPanel extends Pane {
	private final List<Integer> usedNews = new ArrayList<>();
	private static final String FONT = "SUIT";
	private static final int MAX_TURN = 20;
	private Runnable gameFinishedHandler;
	private boolean gameEnded = false;
	private final javafx.scene.layout.StackPane card;
	private final Label timeLabel;
	private final Label turnLabel;
	private final Label titleLabel;
	private final Label contentLabel;
	private final ImageView newsImage;

	private int turn = 0;
	private int currentEffect = 0;
	private int elapsedSeconds = 0;
	private int lastIndex = -1;

	private boolean gameStarted = false;

	private String currentNewsTitle = "";

	private NewsListener listener;
	private Consumer<ImageView> imageClickHandler;

	private final Random random = new Random();

	private Timeline clockTimer;
	private Timeline newsTimer;

	private static class NewsItem {
		String title;
		String content;
		int effect;
		String imagePath;

		NewsItem(String title, String content, int effect, String imagePath) {
			this.title = title;
			this.content = content;
			this.effect = effect;
			this.imagePath = imagePath;
		}
	}

	private final NewsItem[] newsList = {
			new NewsItem("한국은행, 기준금리 0.25%p 인하",
					"한국은행 금융통화위원회가 기준금리를 기존 3.00%에서 2.75%로 인하한다고 발표했다. 시장에서는 유동성 확대와 기업 투자 활성화에 대한 기대감이 커지고 있으며, 증권가에서는 주식시장에 긍정적인 영향을 줄 것으로 전망하고 있다.",
					70,
					"/Newsimg/p1.png"),

			new NewsItem("삼성전자 'HBM9' 세계 최초 공개",
					"삼성전자가 차세대 고대역폭 메모리인 HBM9을 공식 발표했다. 기존 제품 대비 데이터 처리 속도와 전력 효율이 크게 향상된 것으로 알려지며 AI 반도체 시장 성장 기대감이 커지고 있다.",
					90,
					"/Newsimg/p2.png"),

			new NewsItem("코스피 4,000선 붕괴... 금융시장 패닉",
					"글로벌 경기 침체 우려와 외국인 투자자의 대규모 매도세가 겹치며 코스피 지수가 장중 4,000선을 하회했다. 투자 심리가 급격히 위축되며 추가 조정 가능성이 제기되고 있다.",
					-70,
					"/Newsimg/p3.png"),

			new NewsItem("신종 코로나28 바이러스 확산 우려",
					"새로운 변이 바이러스인 코로나28 감염 사례가 다수 국가에서 보고되며 글로벌 보건 당국이 비상 대응에 나섰다. 여행, 항공, 유통 업종을 중심으로 투자 심리가 악화되고 있다.",
					-90,
					"/Newsimg/p4.png"),

			new NewsItem("코스피 8,000선 사상 첫 돌파",
					"코스피 지수가 장중 8,000선을 돌파하며 역사상 최고치를 기록했다. AI 산업 성장과 기업 실적 호조, 풍부한 유동성에 힘입어 외국인과 기관 투자자의 대규모 매수세가 이어지고 있다.",
					100,
					"/Newsimg/p5.png"),

			new NewsItem("엔비디아, RTX 6000 시리즈 공식 발표",
					"엔비디아가 차세대 AI 및 데이터센터용 GPU인 RTX 6000 시리즈를 공개했다. 압도적인 연산 성능과 메모리 대역폭 향상으로 반도체 및 기술주 중심의 강한 매수세가 유입되고 있다.",
					80,
					"/Newsimg/p6.png"),

			new NewsItem("미국·이란 무력 충돌 발생",
					"중동 지역 긴장이 최고조에 달하며 미국과 이란 간 군사 충돌이 발생했다. 국제유가가 급등하고 글로벌 증시는 불확실성 확대에 따른 매도세를 보이고 있다.",
					-50,
					"/Newsimg/p7.png"),

			new NewsItem("혁신 신약 임상 성공",
					"글로벌 제약사가 암 완치율을 크게 높인 신약 개발에 성공했다. 바이오 업종 전반에 투자자들의 관심이 집중되며 관련 종목들이 강세를 보이고 있다.",
					80,
					"/Newsimg/p8.png"),

			new NewsItem("세계 경제 성장률 전망 상향",
					"국제통화기금이 세계 경제 성장률 전망치를 상향 조정했다. 글로벌 증시가 일제히 상승하며 위험자산 선호 심리가 강화되고 있다.",
					90,
					"/Newsimg/p9.png"),

			new NewsItem("글로벌 금융위기 발생",
					"주요 금융기관의 유동성 위기와 세계 증시 폭락이 이어지면서 글로벌 금융위기 우려가 현실화되고 있다. 투자자들은 위험자산을 대거 매도하고 있다.",
					-100,
					"/Newsimg/p10.png"),

			new NewsItem("애플, AI 전용 칩 공개",
					"애플이 자체 개발한 차세대 AI 전용 칩을 공개했다. 온디바이스 AI 성능이 크게 향상될 것으로 예상되며 글로벌 기술주 전반에 긍정적인 분위기가 형성되고 있다.",
					70,
					"/Newsimg/p11.png"),

			new NewsItem("전고체 배터리 상용화 성공",
					"차세대 전고체 배터리 양산 기술이 확보되며 전기차 산업의 새로운 성장 동력이 마련됐다. 배터리와 전기차 관련 종목에 강한 매수세가 유입되고 있다.",
					90,
					"/Newsimg/p12.png"),

			new NewsItem("미국 AI 산업 지원법 통과",
					"미국 의회가 AI 산업 육성 법안을 통과시켰다. 데이터센터, 반도체, 클라우드 산업에 대한 대규모 투자가 예상되며 기술주 중심의 강세장이 이어지고 있다.",
					80,
					"/Newsimg/p13.png"),

			new NewsItem("외계 생명체 발견 가능성 발표",
					"국제 연구진이 태양계 외부 행성에서 생명체 존재 가능성을 확인했다고 발표했다. 우주항공, 통신, 첨단 기술 산업에 대한 투자 기대감이 급격히 확대되고 있다.",
					120,
					"/Newsimg/p14.png"),

			new NewsItem("세계 최초 핵융합 발전 상용화",
					"핵융합 발전소가 상업 운전에 성공하며 에너지 산업의 패러다임 변화가 시작되고 있다. 저비용 청정에너지 시대가 열릴 것이라는 기대감에 관련 기업들이 급등하고 있다.",
					120,
					"/Newsimg/p15.png"),

			new NewsItem("대형 은행 파산",
					"글로벌 대형 은행의 유동성 위기가 현실화되며 금융시장 불안이 확대되고 있다. 투자자들은 위험자산을 회피하고 있으며 증시 전반에 매도세가 나타나고 있다.",
					-120,
					"/Newsimg/p16.png"),

			new NewsItem("AI 버블 붕괴 우려",
					"AI 관련 기업들의 과도한 고평가 논란이 커지며 투자 심리가 위축되고 있다. 일부 기술주는 실적 대비 주가가 과도하다는 분석에 급락세를 보이고 있다.",
					-80,
					"/Newsimg/p17.png"),

			new NewsItem("초대형 태양폭풍 발생",
					"초대형 태양폭풍으로 전 세계 통신망과 데이터센터 일부가 장애를 겪고 있다. 기술주와 클라우드 관련 기업을 중심으로 매도세가 확대되고 있다.",
					-100,
					"/Newsimg/p18.png"),

			new NewsItem("글로벌 경기 침체 공식 선언",
					"주요국 경제 성장률이 마이너스로 전환되며 세계 경기 침체가 공식화됐다. 소비 둔화와 기업 실적 악화 우려로 글로벌 증시가 동반 하락하고 있다.",
					-150,
					"/Newsimg/p19.png"),

			new NewsItem("국제유가 사상 최고치 돌파",
					"중동 정세 불안과 공급 차질 우려가 겹치며 국제유가가 사상 최고치를 기록했다. 물가 상승 압력이 커지면서 기업 비용 부담과 소비 위축 우려가 확대되고 있다.",
					-90,
					"/Newsimg/p20.png")
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
		newsImage.setFitHeight(330);
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
		titleLabel.setLayoutY(470);

		contentLabel.setLayoutX(30);
		contentLabel.setLayoutY(520);

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
		gameEnded = false;
		usedNews.clear();
		gameStarted = true;
		elapsedSeconds = 0;
		turn = 0;
		lastIndex = -1;

		timeLabel.setText("00:00");
		turnLabel.setText("TURN 0");

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
				new KeyFrame(Duration.minutes(1), e -> nextTurn())
		);

		newsTimer.setCycleCount(Timeline.INDEFINITE);
		newsTimer.play();
	}

	public void nextTurn() {
		if (!gameStarted || gameEnded) {
			return;
		}

		if (turn >= MAX_TURN) {
			gameEnded = true;
			stopNewsTimer();

			turnLabel.setText("TURN " + MAX_TURN);
			titleLabel.setText("게임 종료!");
			contentLabel.setText("20턴이 종료되었습니다. 최종 결과를 확인하세요.");

			currentNewsTitle = "게임 종료!";
			currentEffect = 0;

			if (listener != null) {
				listener.onNewsChanged(
						currentNewsTitle,
						"20턴이 종료되었습니다. 최종 결과를 확인하세요.",
						currentEffect,
						turn
				);
			}

			if (gameFinishedHandler != null) {
				gameFinishedHandler.run();
			}
			return;
		}

		turn++;
		turnLabel.setText("TURN " + turn);

		NewsItem item = getRandomNews();

		currentNewsTitle = item.title;
		currentEffect = item.effect;

		titleLabel.setText(item.title);
		contentLabel.setText(item.content);

		loadNewsImage(item.imagePath);

		if (listener != null) {
			listener.onNewsChanged(
					item.title,
					item.content,
					item.effect,
					turn
			);
		}
	}
	
	public void setGameFinishedHandler(Runnable gameFinishedHandler) {
		this.gameFinishedHandler = gameFinishedHandler;
	}
	
	private NewsItem getRandomNews() {

	    if (usedNews.size() >= newsList.length) {
	        usedNews.clear();
	    }

	    int index;

	    do {
	        index = random.nextInt(newsList.length);
	    } while (usedNews.contains(index));

	    usedNews.add(index);

	    return newsList[index];
	}

	private void stopNewsTimer() {
		if (newsTimer != null) {
			newsTimer.stop();
		}

		if (clockTimer != null) {
			clockTimer.stop();
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
	public void resetGame() {
	    turn = 0;
	    elapsedSeconds = 0;
	    gameEnded = false;
	    gameStarted = false;

	    titleLabel.setText("뉴스 준비 중");
	    contentLabel.setText("상단 카운트다운이 끝나면 뉴스가 표시됩니다.");
	    timeLabel.setText("00:00");
	    turnLabel.setText("TURN 0");

	    newsImage.setImage(null);
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