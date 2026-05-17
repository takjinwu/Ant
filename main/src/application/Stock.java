package application;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleDoubleProperty;

public class Stock {

	private final String name;
	private final String code;
	private final double basePrice;
	private final SimpleDoubleProperty price;
	private final List<Double> history = new ArrayList<>();

	public Stock(String name, String code, double initialPrice) {
		this.name = name;
		this.code = code;
		this.basePrice = initialPrice;
		this.price = new SimpleDoubleProperty(initialPrice);
		this.history.add(initialPrice);
	}

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}

	public double getBasePrice() {
		return basePrice;
	}

	public double getPrice() {
		return price.get();
	}

	public SimpleDoubleProperty priceProperty() {
		return price;
	}

	public List<Double> getHistory() {
		return history;
	}

	public double getChangePercent() {
		if (history.isEmpty()) {
			return 0;
		}
		double start = history.get(0);
		if (start == 0) {
			return 0;
		}
		return (getPrice() - start) / start * 100.0;
	}

	public double getLastChangePercent() {
		int n = history.size();
		if (n < 2) {
			return 0;
		}
		double prev = history.get(n - 2);
		if (prev == 0) {
			return 0;
		}
		return (getPrice() - prev) / prev * 100.0;
	}

	public void applyChange(double percent) {
		double next = getPrice() * (1.0 + percent / 100.0);
		next = Math.max(100, next);
		next = Math.round(next / 10.0) * 10.0;
		price.set(next);
		history.add(next);
	}
}
