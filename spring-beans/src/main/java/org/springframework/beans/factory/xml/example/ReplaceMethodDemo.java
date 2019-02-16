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

import org.springframework.beans.factory.support.MethodReplacer;

/**
 * @author hejianglong
 * @date 2019-02-16 15:50
 * @email hejlong@163.com
 * @Desc
 */
public class ReplaceMethodDemo {

	public static void main(String[] args) {
		// ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring.xml");
		// Method method = (Method) context.getBean("method");
		// method.display();

		/**
		 * <bean id="methodReplace" class="org.springframework.core.test1.MethodReplace"/>
		 *
		 * <bean id="method" class="org.springframework.core.test1.Method"/>
		 *
		 * System.out.println("我是原始方法");
		 */

		/**
		 * <bean id="methodReplace" class="org.springframework.core.test1.MethodReplace"/>
		 *
		 * <bean id="method" class="org.springframework.core.test1.Method">
		 *
		 *     <replaced-method name="display" replacer="methodReplace"/>
		 *
		 * </bean>
		 *
		 * System.out.println("我是替换方法");
		 */
	}
}

class Method {

	public void display() {
		System.out.println("我是原始方法");
	}
}

class MethodReplace implements MethodReplacer {

	@Override
	public Object reimplement(Object obj, java.lang.reflect.Method method, Object[] args) throws Throwable {
		System.out.println("我是替换方法");
		return null;
	}
}