/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.xml;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.tests.sample.beans.test.BeanPostProcessorTest;
import org.xml.sax.InputSource;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.ObjectUtils;

import static org.junit.Assert.*;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class XmlBeanDefinitionReaderTests {

	@Test
	public void setParserClassSunnyDay() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		new XmlBeanDefinitionReader(registry).setDocumentReaderClass(DefaultBeanDefinitionDocumentReader.class);
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void withOpenInputStream() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		Resource resource = new InputStreamResource(getClass().getResourceAsStream("test.xml"));
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
	}

	@Test
	public void withOpenInputStreamAndExplicitValidationMode() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		Resource resource = new InputStreamResource(getClass().getResourceAsStream("test.xml"));
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_DTD);
		reader.loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	@Test
	public void withImport() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		Resource resource = new ClassPathResource("import.xml", getClass());
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	@Test
	public void withWildcardImport() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		Resource resource = new ClassPathResource("importPattern.xml", getClass());
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void withInputSource() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		InputSource resource = new InputSource(getClass().getResourceAsStream("test.xml"));
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
	}

	@Test
	public void withInputSourceAndExplicitValidationMode() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		InputSource resource = new InputSource(getClass().getResourceAsStream("test.xml"));
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_DTD);
		reader.loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	@Test
	public void withFreshInputStream() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		Resource resource = new ClassPathResource("test.xml", getClass());
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	private void testBeanDefinitions(BeanDefinitionRegistry registry) {
		assertEquals(24, registry.getBeanDefinitionCount());
		assertEquals(24, registry.getBeanDefinitionNames().length);
		assertTrue(Arrays.asList(registry.getBeanDefinitionNames()).contains("rod"));
		assertTrue(Arrays.asList(registry.getBeanDefinitionNames()).contains("aliased"));
		assertTrue(registry.containsBeanDefinition("rod"));
		assertTrue(registry.containsBeanDefinition("aliased"));
		assertEquals(TestBean.class.getName(), registry.getBeanDefinition("rod").getBeanClassName());
		assertEquals(TestBean.class.getName(), registry.getBeanDefinition("aliased").getBeanClassName());
		assertTrue(registry.isAlias("youralias"));
		String[] aliases = registry.getAliases("aliased");
		assertEquals(2, aliases.length);
		assertTrue(ObjectUtils.containsElement(aliases, "myalias"));
		assertTrue(ObjectUtils.containsElement(aliases, "youralias"));
	}

	@Test
	public void dtdValidationAutodetect() {
		doTestValidation("validateWithDtd.xml");
	}

	@Test
	public void xsdValidationAutodetect() {
		doTestValidation("validateWithXsd.xml");
	}

	private void doTestValidation(String resourceName) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		Resource resource = new ClassPathResource(resourceName, getClass());
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(resource);
		TestBean bean = (TestBean) factory.getBean("testBean");
		assertNotNull(bean);
	}

	@Test
	public void withIocLoadBeanDefinition() {
		Resource resource = new ClassPathResource("test.xml", getClass());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions(resource);
		Object obj = beanFactory.getBean("validEmptyWithDescription");
		Assert.assertNotNull(obj);
	}

	/**
	 * 解决单列实例循环依赖问题
	 * 只能解决 filed 注入的方式
	 * 构造器注入不能解决原型模式也不能解决
	 *
	 * AbstractBeanFactory#createBean
	 * -> AbstractAutowireCapableBeanFactory#doCreateBean
	 * -> AbstractAutowireCapableBeanFactory#addSingletonFactory
	 * 关键处 this.singletonFactories.put(beanName, singletonFactory);
	 * 当 Bean A 在此处加入到 singletonFactories 中
	 * -> AbstractAutowireCapableBeanFactory#populateBean(beanName, mbd, instanceWrapper); 进行属性赋值
	 * -> AbstractAutowireCapableBeanFactory#applyPropertyValues(beanName, mbd, bw, pvs); 将之前获取到的属性进行注入到实例属性上
	 * Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue); 这段代码会将取出的 Bean b 的值转换成 IOC 容器
	 * 中对应的引用赋值给当前字段，意味着需要去实例化 Bean B
	 * 如果是 RuntimeBeanReference 然后调用 resolveReference(argName, ref);
	 * 然后调用 bean = this.beanFactory.getBean(refName); 此处回去先实列化 B 对象
	 * 然后调用回到 AbstractBeanFactory，Object sharedInstance = getSingleton(beanName);
	 * 此处从 singletonFactories 取出值 ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
	 * singletonObject = singletonFactory.getObject();
	 * this.earlySingletonObjects.put(beanName, singletonObject);
	 * 此处返回不为 null 经过 bean = getObjectForBeanInstance(sharedInstance, name, beanName, null); 来完成实例化 Bean
	 * 后方法直接返回走后续流程
	 * 到达后续循环依赖处理 if (earlySingletonExposure) { ... }
	 *
	 */
	@Test
	public void withIocCircularDepend() {
		Resource resource = new ClassPathResource("test.xml", getClass());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions(resource);
		Object obj = beanFactory.getBean("a");
		Assert.assertNotNull(obj);
	}

	/**
	 * 在 Bean 属性填充赋值之后
	 * 调用初始化方法之前（InitializingBean#afterPropertiesSet()，init-method）
	 * 其实就是 Spring 容器检测到当前 bean 是否实现了 Aware 接口
	 * 然后看其具体实现的接口依次调用 setXxx() 方法设置 BeanName、BeanClassLoader、BeanFactory
	 */
	@Test
	public void testIocAware() {
		Resource resource = new ClassPathResource("test.xml", getClass());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions(resource);
		Object obj = beanFactory.getBean("myApplicationAware");
		Assert.assertNotNull(obj);
	}

	/**
	 * BeanPostProcessor
	 * 会在调用 AbstractAutowireCapableBeanFactory#initializeBean() 初始化方法中调用
	 * 在 bean 实例化后
	 * 初始化前、后调用
	 * 一般的 BeanFactory 不支持自动注册 BeanPostProcessor 需要我们手动调用
	 * 如果我们需要 bean 进行一些配置，增加一些自己的逻辑，那么请使用 BeanPostProcessor
	 * 通过设置了 BeanPostProcessor 在对应的时机回调，它是 Spring 提供的对象实例化阶段强有力的拓展点
	 * 允许 Spring 在实例化(初始化阶段)对其进行定制化修改，
	 * 比较常见的使用场景是处理标记接口实现类或者为当前对象提供代理实现（例如 AOP）
	 * addBeanPostProcessor() 方法进行注册，注册后的 BeanPostProcessor 适用于所有该 BeanFactory 创建的 bean
	 *
	 * 但是 ApplicationContext 可以在其 bean 定义中自动检测所有的 BeanPostProcessor 并自动完成注册，
	 * 同时将他们应用到随后创建的任何的 Bean 中
	 * 它其中的 BeanPostProcessor 可以进行排序，而 BeanFactoryProcessor 中的顺序只和加入的顺序有关
	 * 实例化 bean -> 激活 Aware -> BeanPostProcessor 前置处理 -> 初始化 bean -> BeanPostProcessor 后置处理
	 */
	@Test
	public void testIocBeanPostProcessor() {
		Resource resource = new ClassPathResource("test.xml", getClass());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions(resource);
		beanFactory.addBeanPostProcessor(new BeanPostProcessorTest());
		BeanPostProcessorTest beanPostProcessorTest = (BeanPostProcessorTest) beanFactory.getBean("beanPostProcessorTest");
		Assert.assertNotNull(beanPostProcessorTest);
	}

}
