package org.springframework.tests.sample.beans.test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;

public class MyApplicationAware implements BeanNameAware, BeanFactoryAware, BeanClassLoaderAware {

	private String beanName;

	private BeanFactory beanFactory;

	private ClassLoader classLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		System.out.println("调用了 setBeanClassLoader");
		this.classLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		System.out.println("调用了 setBeanFactory");
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanName(String name) {
		System.out.println("调用了 setBeanName");
		this.beanName = name;
	}
}
