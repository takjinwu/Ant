package application;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public class Portfolio {

	public static final double INITIAL_CASH = 10_000_000.0;

	private final SimpleDoubleProperty cash = new SimpleDoubleProperty(INITIAL_CASH);
	private final ObservableMap<Stock, SimpleIntegerProperty> holdings =
			FXCollections.observableMap(new LinkedHashMap<>());
	private final Map<Stock, Double> avgBuyPrice = new LinkedHashMap<>();

	public SimpleDoubleProperty cashProperty() {
		return cash;
	}

	public double getCash() {
		return cash.get();
	}

	public ObservableMap<Stock, SimpleIntegerProperty> getHoldings() {
		return holdings;
	}

	public int getQuantity(Stock stock) {
		SimpleIntegerProperty q = holdings.get(stock);
		return q == null ? 0 : q.get();
	}

	public double getAvgBuyPrice(Stock stock) {
		Double p = avgBuyPrice.get(stock);
		return p == null ? 0 : p;
	}

	public boolean buy(Stock stock, int qty) {
		if (qty <= 0) {
			return false;
		}
		double cost = stock.getPrice() * qty;
		if (cost > cash.get()) {
			return false;
		}

		cash.set(cash.get() - cost);

		int curQty = getQuantity(stock);
		double curAvg = getAvgBuyPrice(stock);
		double newAvg = (curAvg * curQty + stock.getPrice() * qty) / (curQty + qty);
		avgBuyPrice.put(stock, newAvg);

		SimpleIntegerProperty prop = holdings.get(stock);
		if (prop == null) {
			prop = new SimpleIntegerProperty(0);
			holdings.put(stock, prop);
		}
		prop.set(curQty + qty);

		return true;
	}

	public boolean sell(Stock stock, int qty) {
		if (qty <= 0) {
			return false;
		}
		int curQty = getQuantity(stock);
		if (qty > curQty) {
			return false;
		}

		cash.set(cash.get() + stock.getPrice() * qty);

		int remaining = curQty - qty;
		holdings.get(stock).set(remaining);
		if (remaining == 0) {
			avgBuyPrice.remove(stock);
			holdings.remove(stock);
		}

		return true;
	}

	public double getEvaluation() {
		double v = 0;
		for (Map.Entry<Stock, SimpleIntegerProperty> e : holdings.entrySet()) {
			v += e.getKey().getPrice() * e.getValue().get();
		}
		return v;
	}

	public double getTotalAsset() {
		return cash.get() + getEvaluation();
	}

	public double getReturnPercent() {
		return (getTotalAsset() - INITIAL_CASH) / INITIAL_CASH * 100.0;
	}
}
