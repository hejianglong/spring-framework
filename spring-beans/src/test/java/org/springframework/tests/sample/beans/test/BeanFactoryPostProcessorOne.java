package org.springframework.tests.sample.beans.test;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;

public class BeanFactoryPostProcessorOne implements BeanFactoryPostProcessor, Ordered {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("BeanFactoryPostProcessor one...");
		System.out.println("容器中 BeanDefinition 的个数: " + beanFactory.getBeanDefinitionCount());

		BeanDefinition beanDefinition = beanFactory.getBeanDefinition("studentService");
		MutablePropertyValues pvs = beanDefinition.getPropertyValues();
		pvs.addPropertyValue("name", "along1");
		pvs.add("age", 27);
	}


	@Override
	public int getOrder() {
		return 1;
	}
}
