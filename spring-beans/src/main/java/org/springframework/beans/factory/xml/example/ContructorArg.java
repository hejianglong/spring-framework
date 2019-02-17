/*
 * Project: org.springframework.beans.factory.xml.example
 *
 * File Created at 2019-02-17
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
 * @author hejianglong
 * @date 2019-02-17 16:09
 * @email hejlong@163.com
 * @Desc
 */
public class ContructorArg {

	/**
	 * 用法
	 *
	 * <bean id = "bookService" class = "org.springframework.beans.factory.xml.example.BookService" />
	 *
	 * <bean id = "studentService" class = "org.springframework.beans.factory.xml.example.StudentService">
	 *     <constructor-arg index = "0" value = "chenssy" />
	 *     <constructor-arg name = "age" value = "100" />
	 *     <constructor-arg name = "bookService" ref = "bookService" />
	 * </bean>
	 */

}
class StudentService {

	private String name;

	private Integer age;

	private BookService bookService;

	public StudentService(String name, Integer age, BookService bookService) {
		this.name = name;
		this.age = age;
		this.bookService = bookService;
	}
}
class BookService {

}