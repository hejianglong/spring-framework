package test;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.sample.beans.test.Student;
import org.springframework.tests.sample.beans.test.StudentService;

public class BeanTest {

	/**
	 * PropertyPlaceholderConfigurer 的用法
	 *
	 * 当我们 xml 配置文件中存在 ${} 取数据的时候，如果我们配置了实现 PropertyPlaceholderConfigurer 的接口
	 * 就会去调用 loadProperties 它的实现原理如下。
	 *
	 * 从 PropertyPlaceholderConfigurer  的结构图可以看出，它间接实现了 Aware 和 BeanFactoryPostProcessor 两大接口
	 * 这里只需要关注 BeanFactoryPostProcessor 即可，因为它提供了 postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) 方法
	 * 因为它触发时机在：BeanDefinition 加载完成后，实例化初始化前
	 *
	 * 自定义的 loadProperties 调用时机在
	 * PropertyResourceConfigurer#postProcessBeanFactory
	 */
	@Test
	public void testPropertyPlaceholderConfigurer() {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring_2.xml");

		StudentService studentService = (StudentService) context.getBean("studentService");
		System.out.println(studentService.getName());
		Assert.assertNotNull(studentService);
	}

	/**
	 * 可以通过 PropertyOverrideConfigurer 来覆盖任何 bean 中的任何属性
	 */
	@Test
	public void testPropertyOverrideConfigurer() {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring_3.xml");

		StudentService studentService = (StudentService) context.getBean("student");
		System.out.println(studentService.getName());
		Assert.assertNotNull(studentService);
	}

	/**
	 * ConversionService 是 Spring 类型转换器体系中的核心接口，它定义了是否可以完成转换与类型转换两类接口方法
	 *
	 * 如何自定义类型转换器
	 * 1. 实现 Converter / GenericConverter / ConverterFactory 接口
	 * 2. 将该类注册到 ConversionServiceFactoryBean 中
	 *
	 * ConversionServiceFactoryBean 实现了 InitializingBean 接口实现 #afterPropertiesSet() 方法
	 * 它会在实例化 Bean 完成调用 Aware 接口和 BeanPostProcessor 前置处理器后调用
	 * 此处会设置好我们配置的自定义类型转换器放入 GenericConversionService 的 converters 中
	 *
	 * 当我们实列化对应的类对其注入属性值的时候，例如 student
	 * 会从 converters 取出自定义的类型转换器然后调用其 convert 方法，放入 Student 的 PropertyValue 中的 convertValue 中
	 * 来完成属性值的类型转换及注入
	 */
	@Test
	public void testConversion() {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring_4.xml");
		Student student = (Student) context.getBean("student");
		System.out.println(student);
		Assert.assertNotNull(student);
	}
}
