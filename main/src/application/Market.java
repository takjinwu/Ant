package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Market {

	private final ObservableList<Stock> stocks = FXCollections.observableArrayList();
	private final SimpleObjectProperty<Stock> selected = new SimpleObjectProperty<>();
	private final SimpleIntegerProperty turnCounter = new SimpleIntegerProperty(0);
	private final Random random = new Random();

	public Market() {
		stocks.add(new Stock("삼성전자", "005930", 70000));
		stocks.add(new Stock("SK하이닉스", "000660", 130000));
		stocks.add(new Stock("LG에너지솔루션", "373220", 450000));
		stocks.add(new Stock("NAVER", "035420", 200000));
		stocks.add(new Stock("카카오", "035720", 50000));
		stocks.add(new Stock("현대차", "005380", 190000));
		selected.set(stocks.get(0));
	}

	public ObservableList<Stock> getStocks() {
		return stocks;
	}

	public SimpleObjectProperty<Stock> selectedProperty() {
		return selected;
	}

	public Stock getSelected() {
		return selected.get();
	}

	public void setSelected(Stock s) {
		selected.set(s);
	}

	public SimpleIntegerProperty turnCounterProperty() {
		return turnCounter;
	}

	public void applyNewsEffect(int effect) {
		for (Stock s : stocks) {
			double jitter = (random.nextDouble() - 0.5) * 4.0;
			s.applyChange(effect + jitter);
		}
		turnCounter.set(turnCounter.get() + 1);
	}

	public List<Stock> getList() {
		return new ArrayList<>(stocks);
	}
}
