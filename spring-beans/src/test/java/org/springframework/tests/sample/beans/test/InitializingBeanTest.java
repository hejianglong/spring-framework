package org.springframework.tests.sample.beans.test;

import org.springframework.beans.factory.InitializingBean;

public class InitializingBeanTest implements InitializingBean {

	private String name;

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("InitializingBeanTest initializing...");
		this.name = "hello - 2";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOtherName() {
		System.out.println("InitializingBeanTest setOtherName");
		this.name = "hello - 3";
	}
}
