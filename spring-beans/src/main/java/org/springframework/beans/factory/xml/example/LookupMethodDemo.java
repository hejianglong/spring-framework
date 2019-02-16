/*
 * Project: org.springframework.beans.factory.xml.example
 *
 * File Created at 2019-02-16
 *
 * Copyright 2019 CMCC Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * ZYHY Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license.
 */
package org.springframework.beans.factory.xml.example;

/**
 * 演示 lookup-method 用法
 * @author hejianglong
 * @date 2019-02-16 14:43
 * @email hejlong@163.com
 * @Desc
 */
public class LookupMethodDemo {

	public static void main(String[] args) {

		// ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring.xml");
		// Display display = (Display) context.getBean("display");
		// display.display();
		// 输出: Hongqi ...

		/**
		 * spring.xml content
		 *
		 * <bean id="display" class="org.springframework.core.test1.Display">
		 *     <lookup-method name="getCar" bean="hongqi"/>
		 * </bean>
		 */
	}
}

interface Car {
	void display();
}

class Bmw implements Car {

	@Override
	public void display() {
		System.out.println("Bmw ...");
	}
}

class Hongqi implements Car {

	@Override
	public void display() {
		System.out.println("Hongqi ...");
	}
}

abstract class Display {
	public void display() {
		getCar().display();
	}

	public abstract Car getCar();
}