package org.springframework.test.annotation;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.annotation.test.TestA;

/**
 * IOC 四大体系
 * 1. Resource：代表一个资源，子类有 FileSystem、Classpath、ByteArray 等等代表了不同的资源
 * 		具有方法，获取一个文件、判断是否刻度、获取文件名、文件描述等等
 *
 * 2. ResourceLoader：负责装载 Resource，子类有 DefaultResourceLoader，ResourcePatternResolver
 * 		具有方法，getResource(String location) 根据路径装载 resource 然后返回 Resource 对象
 *
 * 3. BeanDefinition：bean 定义包含了创建一个 Bean 所需要的全部信息
 *
 * -------- 从 Resource -> BeanDefinition，当我们使用 scan basePackages 的时候，会使用
 * -------- ResourcePatternResolver 去解析包下面的所有的类将他们解析为 Resource[]，然后遍历每一个 Resource 来创建 BeanDefinition
 *
 * BeanDefinitionRegistry：该接口定义了注册 Bean 到容器中及删除、获取 BeanDefinition 等方法
 *
 * -------- Resource 解析为 BeanDefinition 后，由 BeanDefinitionRegistry 将 BeanDefinition 放入到对应的 Map 容器中备用
 *
 * 4. BeanFactory：bean 工具，该接口定义了获取 Bean，获取 Bean 类型，Bean 的 scope 判断等，子类实现有 ListableBeanFactory、ApplicationContext 等
 * DefaultListableBeanFactory：该类实现了 BeanFactory、BeanDefinition 等，他实现了二者的功能， BD 将会被维护到自身的 beanDefinitionMap 中
 * 同时提供了 getBean(String beanName) 等方法，如果 bean 没有创建则回去创建 Bean
 *
 * ApplicationContext：该接口是 BeanFactory 的升级版，它在实现了 BeanFactory 的同时增加了些其它的支持
 * 		实现了 ResourcePatternResolver 接口支持模糊匹配加载类文件
 * 		实现了 ApplicationEventPublisher	 支持事件的发布和订阅功能
 * 		实现了 MessageSource 支持国际化
 * 		实现了 ListableBeanFactory 具备基础 BeanFactory 的功能
 *
 *
 * @author hejianglong
 * @date 2019/11/11
 */
public class AnnotationTest {

	/**
	 * 1. 初始化 AnnotatedBeanDefinitionReader、ClassPathBeanDefinitionScanner
	 * 2. ClassPathBeanDefinitionScanner#doScan(String... basePackages)
	 * 3. 通过 ResourcePatternResolver 根据传入的 basePackages 获得指定你包下面的 Resource[]
	 * 4. 遍历 Resource 来创建对应的 BeanDefinition
	 * 5. 然后遍历获取到的所有的 BeanDefinition，调用 BeanDefinitionRegistry#registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
	 * 将对应的 BeanDefinition 放入 beanDefinitionMap 中，beanName 为 key
	 * 6. 调用 AbstractApplicationContext#refresh() 这是一个核心方法，调用后所有非懒加载的 Bean 都将被实例化
	 *
	 * refresh()
	 * 1. AbstractApplicationContext#invokeBeanFactoryPostProcessors(beanFactory) 调用 BeanFactoryPostProcessor 此时 BeanDefinition
	 * 已经加载完毕，提供最后一次修改 BeanDefinition 的机会
	 * 1.1 PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
	 * 1.2 先调用 BeanDefinitionRegistryPostProcessor，按照 PriorityOrdered、Ordered 顺序执行然后执行剩下的 BeanDefinitionRegistryPostProcessor
	 * 1.3 然后调用 BeanFactoryPostProcessor，按照 PriorityOrdered、Ordered、NonOrdered（没有顺序）执行
	 *
	 * 2. AbstractApplicationContext#registerBeanPostProcessors(beanFactory)
	 * 将 BeanPostProcessor 按照顺序注册到 BeanFactory 中，便于后续在 Bean 初始化前或者后进行调用
	 * 2.1 PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
	 * 2.2 对 BeanPostProcessor 按照 PriorityOrdered、Ordered、NonOrdered 的顺序依次放入 BeanFactory 中，同事将内部使用的 BeanPostProcessor 放到集合尾部
	 *
	 * 3. AbstractApplicationContext#finishBeanFactoryInitialization(beanFactory); 实列化所有非 lazy 的 Bean
	 * 3.1 DefaultListableBeanFactory#preInstantiateSingletons(); 实列化所有非 lazy 的 Bean
	 * 3.2 遍历 beanDefinitionNames 区分是 FactoryBean 还是普通的 Bean
	 * 3.3 调用 AbstractBeanFactory#getBean(beanName); 进行实例化
	 * 3.4 DefaultSingletonBeanRegistry#getSingleton(String beanName); 从单列缓冲中获取对应的实列 Bean，容器中只会有一份已经存在则直接返回
	 * 同时 earlySingletonObjects 中存放早起的 Bean 可以解决循环引用的问题，但是只能解决单列模式下的循环引用
	 * 3.5 如果缓存中没有获取到的对应的实列 Bean 那么就需要对其进行实例化，DefaultSingletonBeanRegistry#getSingleton(String beanName, ObjectFactory<?> singletonFactory)
	 * 然后调用 AbstractAutowireCapableBeanFactory#createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
	 *		首先获取 Class 然后将其设置到 RootBeanDefinition mbdToUse = mbd 中，通过 AbstractFactory#resolveBeanClass(mbd, beanName); Class.forName() 根据 BeanDefinition 加载 Class 并且返回
	 *		然后后调用 resolveBeforeInstantiation，给 BeanPostProcessors 一个返回代理的机会，直接返回对象后就不进行后续的实例化了
	 *		然后调用 AbstractAutowireCapableBeanFactory#doCreateBean(beanName, mbdToUse, args)
	 * 		然后通过调用 createBeanInstance(beanName, mbd, args); 返回 BeanWrapper 对象，
	 * 			首先会解析出 beanClass，然后调用 instantiateBean(beanName, mbd); 来根据无参构造器创建对象，构造器.newInstance() 创建对象后包装为 BeanWrapper
	 * 3.6 然后通过 BeanWrapper 来创建一个 Bean 实例，如果 earlySingletonExposure 为 true 调用 addSingletonFactory 放入缓存和 factories 中或者从 early 中删除
	 * 3.7 调用 populateBean(beanName, mbd, instanceWrapper); 进行 instanceWrapper 中 Bean 的填充，比如 A 依赖 B，那么此时就需要注入（填充） B
	 * 3.7 然后调用 AutowiredAnnotationBeanPostProcessor#postProcessProperties(PropertyValues pvs, Object bean, String beanName) 进行填充
	 * 		BeanPostProcessor 用于在 Bean 初始化前后可以做一些事情此处的 AutowiredAnnotationBeanPostProcessor 负责将依赖装配到对应的 bean 中
	 * 		通过 findAutowiringMetadata 找出依赖的对象
	 * 3.8 然后调用 InjectionMetaData#inject(bean, beanName, pvs) 进行注入依赖的 bean
	 * 3.9 然后调用 InjectedElement#inject(target, beanName, pvs); 进行注入依赖的 bean
	 * 3.10 然后调用 beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter)
	 * 3.11 然后调用 DefaultListable#doResolveDependency，然后回去调用 beanFactory.getBean(beanName);
	 * 		此时就会去实例化它所依赖的 bean
	 * 		如果依赖的 bean 还有依赖那么就依次递归调用
	 *=====================================
	 * 此处如果存在村换依赖的问题，比如 A 依赖 B，B 依赖 A，spring 解决了在单例模式下的这个问题，就是同过 2 个地方
	 * 1. AbstractAutowireCapableBeanFactory#doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args) 方法中
	 * 		// Eagerly cache singletons to be able to resolve circular references
	 * 		// even when triggered by lifecycle interfaces like BeanFactoryAware.
	 * 		// 解决单列模式的循环依赖问题
	 * 		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
	 * 				isSingletonCurrentlyInCreation(beanName));
	 * 		if (earlySingletonExposure) {
	 * 			if (logger.isTraceEnabled()) {
	 * 				logger.trace("Eagerly caching bean '" + beanName +
	 * 						"' to allow for resolving potential circular references");
	 *                        }
	 * 			// 提前将创建的 bean 实例加入到 singletonFactories 中
	 * 			// 这是为了后期避免循环依赖
	 * 			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));*
	 * 			}
	 * 	这段代码 addSingletonFactory 会将 singletonFactory 放入 singletonFactories
	 *
	 * 		protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
	 * 		Assert.notNull(singletonFactory, "Singleton factory must not be null");
	 * 		synchronized (this.singletonObjects) {
	 * 			if (!this.singletonObjects.containsKey(beanName)) {
	 * 				this.singletonFactories.put(beanName, singletonFactory);
	 * 				this.earlySingletonObjects.remove(beanName);
	 * 				this.registeredSingletons.add(beanName);
	 *          }*
	 *      }
	 * 	}
	 *
	 * 	而在 preInstantiateSingletons 的 Object singletonInstance = getSingleton(beanName); 中
	 *	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
	 * 		Object singletonObject = this.singletonObjects.get(beanName);
	 * 		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
	 * 			synchronized (this.singletonObjects) {
	 * 				singletonObject = this.earlySingletonObjects.get(beanName);
	 * 				if (singletonObject == null && allowEarlyReference) {
	 * 					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
	 * 					if (singletonFactory != null) {
	 * 						singletonObject = singletonFactory.getObject();
	 * 						this.earlySingletonObjects.put(beanName, singletonObject);
	 * 						this.singletonFactories.remove(beanName);
	 *                                        }* 				}
	 *                    }
	 * 		}
	 * 		return singleton    ject;
	 * 	}
	 * 	可以根据之前早期创建的 singletonFactories 返回一个早期的 bean 对象，这样就解决了循环依赖问题
	 *=====================================
	 */
	@Test
	public void testAnnotationApplicationContext() {
		AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext("org.springframework.test.annotation.test");
		TestA testA = (TestA) applicationContext.getBean("testA");
		System.out.println(testA);
	}
}
