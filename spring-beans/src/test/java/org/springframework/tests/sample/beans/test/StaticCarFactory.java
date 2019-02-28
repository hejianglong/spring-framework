package org.springframework.tests.sample.beans.test;

import java.util.HashMap;
import java.util.Map;

public class StaticCarFactory {

	private static Map<String, Car> cars = new HashMap<>();

	static {
		cars.put("audi", new Car("audi", "300000"));
		cars.put("bens", new Car("bens", "400000"));
	}

	public static Car getCar(String name) {
		return cars.get(name);
	}
}
