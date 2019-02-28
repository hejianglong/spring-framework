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

import org.apache.catalina.core.ApplicationContext;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.tests.sample.beans.test.*;
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

	/**
	 * 在完成 bean 的实例化后，并且调用了对应的 Aware 注入后
	 * 然后调用了 BeanPostProcessor 前置处理器后，接着会检验当前 bean 对象是否实现了
	 * InitializingBean 接口，如果是则会调用 afterPropertiesSet() 方法进一步调整 bean 实例对象的状态
	 * AbstractAutowireCapableBeanFactory#invokeInitMethods
	 *
	 * 在这个示例中 afterPropertiesSet() 改变了原有 bean 属性值
	 * 这相当于 Spring 容器又给我提供了一种可改变 bean 实例对象的方法
	 *
	 * 然后再检查是否置顶了 init-method，如果指定了则通过反射机制调用指定的 init-method  方法
	 *
	 * Spring 的一个核心理念就是无侵入性，但是如果我们的业务类实现了这个接口就显得 Spring 容器
	 * 具有侵入性了。所以 Spring 还提供了另外一个种实现的方式：init-method 方法
	 * @see testIocInitMethod()
	 */
	@Test
	public void testIocInitializingBean() {
		Resource resource = new ClassPathResource("test.xml", getClass());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions(resource);
		beanFactory.addBeanPostProcessor(new BeanPostProcessorTest());
		InitializingBeanTest initializingBeanTest = (InitializingBeanTest) beanFactory.getBean("initializingBeanTest");
		Assert.assertEquals("hello - 2", initializingBeanTest.getName());
	}

	/**
	 * 和 testIocInitializingBean 触发的时间一样不过
	 * AbstractAutowireCapableBeanFactory#invokeInitMethods
	 * 先回去调用 afterPropertiesSet() 后回去调用 init-method
	 * 这样就可以无侵入性的实现 bean 实例对象初始化的定制化了
	 * 同时可以使用 <beans> 标签的 default-init-method 来统一指定初始化方法
	 */
	@Test
	public void testIocInitMethod() {
		Resource resource = new ClassPathResource("test.xml", getClass());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions(resource);
		beanFactory.addBeanPostProcessor(new BeanPostProcessorTest());
		InitializingBeanTest initializingBeanTest = (InitializingBeanTest) beanFactory.getBean("initializingBeanTest2");
		Assert.assertEquals("hello - 3", initializingBeanTest.getName());
	}

	/**
	 * 实例工厂方法获取 Bean
	 * 首先获取 car 实例的时候会 this.beanFactory.getBean(factoryBeanName); 获取对应的 factoryBean 如果不在 IOC 容器中需要递归加载
	 * 初次加载缓存中未获取到会进行嗅探以确定对应的工厂方法
	 * Modifier.isStatic(candidate.getModifiers()) 并不是意味着必须是静态方法，实例工厂方法一样返回了检测到的工厂方法
	 * 此处为 getCar(name)。
	 * 然后解析出参数的个数以及对应的值，待确认的工厂方法构造出 ArgumentsHolder
	 * 最后利用反射创建 Bean 对象放入 BeanWrapper 中
	 *
	 * 实例工厂方法基本上类似，只是说无需 this.beanFactory.getBean(factoryBeanName)，只要存在对应的类即可
	 */
	@Test
	public void testIocInstanceFactoryMethod() {
		Resource resource = new ClassPathResource("test.xml", getClass());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions(resource);
		beanFactory.addBeanPostProcessor(new BeanPostProcessorTest());
		Car car = (Car) beanFactory.getBean("car");
		System.out.println(car);
	}

	/**
	 * bean 的生命周期
	 * 1. Spring 容器根据实例化策略实例化 Bean 对象
	 * 2. 看是否实现了 Aware 接口，包括 BeanNameAware，BeanClassLoaderAware，BeanFactoryAware 调用其 setXxx 将其设置到实现类中
	 * 3. 调用 BeanPostProcessor#postProcessBeforeInitialization(Object bean, String beanName) 前置处理器，完成 bean 的前置处理
	 * 4. 然后看是否实现了 InitializingBean 接口，调用其 afterPropertiesSet() 方法设置值
	 * 5. 看起是否定义了 init-method 方法，有的话就调用
	 * 6. 调用 BeanPostProcessor#postProcessAfterInitialization(Object bean, String beanName) 后置处理器，完成 bean 的后置处理
	 * 对象完成了初始化
	 * 7. 在容器关闭前看是否实现了 DisposableBean 接口，有这调用 destroy() 方法
	 * 8. 在容器进行关闭之前，如果 bean 配置了 destroy-method 则调用其指定的方法
	 * bean 的生命周期终结
	 */
	@Test
	public void testBeanLife() {

	}

	/**
	 * 用于容器启动阶段，允许我们在容器实例化 Bean 之前对注册到该容器的 BeanDefinition 做出修改
	 *
	 * 采用 ApplicationContext 就无需手动 postProcessBeanFactory(beanFactory)
	 * 因为他会自动识别配置文件中的 BeanFactoryPostProcessor，并且完成注册和调用，而 BeanFactory 就不行
	 * 一般情况下无需主动去自定义 BeanFactoryPostProcessor，Spring 提供了几个常用的 BeanFactoryPostProcessor
	 * PropertyPlaceholderConfigurer、PropertyOverrideConfigurer
	 * PropertyPlaceholderConfigurer：允许我们在 XML 文件中使用占位符并将这些占位符所代表的资源单独配置到简单的
	 * properties 文件来加载。
	 * PropertyOverrideConfigurer：允许我们使用占位符来明确 bean 定义中的 property 与 properties 文件中的各配置
	 * 之间的对应关系，这两个类在大型项目中非常重要
	 */
	@Test
	public void testBeanFactoryPostProcessor() {
		Resource resource = new ClassPathResource("spring_1.xml", getClass());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.loadBeanDefinitions(resource);
		beanFactory.addBeanPostProcessor(new BeanPostProcessorTest());

		BeanFactoryPostProcessorOne one = new BeanFactoryPostProcessorOne();
		BeanFactoryPostProcessorTwo two = new BeanFactoryPostProcessorTwo();
		one.postProcessBeanFactory(beanFactory);
		two.postProcessBeanFactory(beanFactory);

		// ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring_1.xml", getClass());
		StudentService studentService = (StudentService) beanFactory.getBean("studentService");
		Assert.assertNotNull(studentService);
	}
}
