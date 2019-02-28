package org.springframework.tests.sample.beans.test;

import java.util.HashMap;
import java.util.Map;

public class InstanceCarFactory {

	private Map<String, Car> cars = null;

	public InstanceCarFactory() {
		cars = new HashMap<>();
		cars.put("BMW", new Car("BMW", "500000"));
		cars.put("Audi", new Car("Audi", "300000"));
	}

	public Car getCar(String name) {
		return cars.get(name);
	}
}
