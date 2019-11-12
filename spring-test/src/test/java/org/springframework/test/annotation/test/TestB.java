package org.springframework.test.annotation.test;

import org.springframework.stereotype.Component;

/**
 * @author hejianglong
 * @date 2019/11/11
 */
@Component
public class TestB {

	private int c;

	private String url;

	public int getC() {
		return c;
	}

	public void setC(int c) {
		this.c = c;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
