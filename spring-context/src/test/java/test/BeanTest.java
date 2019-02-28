package test;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
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
}
