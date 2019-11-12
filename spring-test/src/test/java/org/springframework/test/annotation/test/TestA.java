package org.springframework.test.annotation.test;

import org.springframework.stereotype.Component;

/**
 * @author hejianglong
 * @date 2019/11/11
 */
@Component
public class TestA {

	private int name;

	private int age;

	public int getName() {
		return name;
	}

	public void setName(int name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
}
