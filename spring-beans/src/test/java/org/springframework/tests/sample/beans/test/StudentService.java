package org.springframework.tests.sample.beans.test;

public class StudentService {

	private String name;

	private int age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "StudentService{" +
				"name='" + name + '\'' +
				", age=" + age +
				'}';
	}
}
