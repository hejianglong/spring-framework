/*
 * Project: org.springframework.tests.sample.beans.test
 *
 * File Created at 2019-03-02
 *
 * Copyright 2019 CMCC Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * ZYHY Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license.
 */
package org.springframework.tests.sample.beans.test;

/**
 * @author hejianglong
 * @date 2019-03-02 10:52
 * @email hejlong@163.com
 * @Desc
 */
public abstract class Display {

	public void display() {
		getCar().display();
	}

	public abstract ICar getCar();
}