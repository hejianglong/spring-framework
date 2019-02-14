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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.EmptyReaderEventListener;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.NullSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.springframework.util.xml.XmlValidationModeDetector;

/**
 * Bean definition reader for XML bean definitions.
 * Delegates the actual XML document reading to an implementation
 * of the {@link BeanDefinitionDocumentReader} interface.
 *
 * <p>Typically applied to a
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
 * or a {@link org.springframework.context.support.GenericApplicationContext}.
 *
 * <p>This class loads a DOM document and applies the BeanDefinitionDocumentReader to it.
 * The document reader will register each bean definition with the given bean factory,
 * talking to the latter's implementation of the
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} interface.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @since 26.11.2003
 * @see #setDocumentReaderClass
 * @see BeanDefinitionDocumentReader
 * @see DefaultBeanDefinitionDocumentReader
 * @see BeanDefinitionRegistry
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * Indicates that the validation should be disabled.
	 * 禁用验证模式
	 */
	public static final int VALIDATION_NONE = XmlValidationModeDetector.VALIDATION_NONE;

	/**
	 * Indicates that the validation mode should be detected automatically.
	 * 自动获取验证模式
	 */
	public static final int VALIDATION_AUTO = XmlValidationModeDetector.VALIDATION_AUTO;

	/**
	 * Indicates that DTD validation should be used.
	 * DTD 验证模式
	 */
	public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;

	/**
	 * Indicates that XSD validation should be used.
	 * XSD 验证模式
	 */
	public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;


	/** Constants instance for this class. */
	private static final Constants constants = new Constants(XmlBeanDefinitionReader.class);

	/**
	 * 默认为自动获取验证模式
	 */
	private int validationMode = VALIDATION_AUTO;

	private boolean namespaceAware = false;

	private Class<? extends BeanDefinitionDocumentReader> documentReaderClass =
			DefaultBeanDefinitionDocumentReader.class;

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	private ReaderEventListener eventListener = new EmptyReaderEventListener();

	private SourceExtractor sourceExtractor = new NullSourceExtractor();

	@Nullable
	private NamespaceHandlerResolver namespaceHandlerResolver;

	private DocumentLoader documentLoader = new DefaultDocumentLoader();

	@Nullable
	private EntityResolver entityResolver;

	private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

	/**
	 * XML 验证模式探测器
	 */
	private final XmlValidationModeDetector validationModeDetector = new XmlValidationModeDetector();

	/**
	 * 当前线程正在加载的 EncodedResource 集合
	 */
	private final ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded =
			new NamedThreadLocal<>("XML bean definition resources currently being loaded");


	/**
	 * Create new XmlBeanDefinitionReader for the given bean factory.
	 * @param registry the BeanFactory to load bean definitions into,
	 * in the form of a BeanDefinitionRegistry
	 */
	public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}


	/**
	 * Set whether to use XML validation. Default is {@code true}.
	 * <p>This method switches namespace awareness on if validation is turned off,
	 * in order to still process schema namespaces properly in such a scenario.
	 * @see #setValidationMode
	 * @see #setNamespaceAware
	 */
	public void setValidating(boolean validating) {
		this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
		this.namespaceAware = !validating;
	}

	/**
	 * Set the validation mode to use by name. Defaults to {@link #VALIDATION_AUTO}.
	 * @see #setValidationMode
	 */
	public void setValidationModeName(String validationModeName) {
		setValidationMode(constants.asNumber(validationModeName).intValue());
	}

	/**
	 * Set the validation mode to use. Defaults to {@link #VALIDATION_AUTO}.
	 * <p>Note that this only activates or deactivates validation itself.
	 * If you are switching validation off for schema files, you might need to
	 * activate schema namespace support explicitly: see {@link #setNamespaceAware}.
	 */
	public void setValidationMode(int validationMode) {
		this.validationMode = validationMode;
	}

	/**
	 * Return the validation mode to use.
	 */
	public int getValidationMode() {
		return this.validationMode;
	}

	/**
	 * Set whether or not the XML parser should be XML namespace aware.
	 * Default is "false".
	 * <p>This is typically not needed when schema validation is active.
	 * However, without validation, this has to be switched to "true"
	 * in order to properly process schema namespaces.
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	/**
	 * Return whether or not the XML parser should be XML namespace aware.
	 */
	public boolean isNamespaceAware() {
		return this.namespaceAware;
	}

	/**
	 * Specify which {@link org.springframework.beans.factory.parsing.ProblemReporter} to use.
	 * <p>The default implementation is {@link org.springframework.beans.factory.parsing.FailFastProblemReporter}
	 * which exhibits fail fast behaviour. External tools can provide an alternative implementation
	 * that collates errors and warnings for display in the tool UI.
	 */
	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * Specify which {@link ReaderEventListener} to use.
	 * <p>The default implementation is EmptyReaderEventListener which discards every event notification.
	 * External tools can provide an alternative implementation to monitor the components being
	 * registered in the BeanFactory.
	 */
	public void setEventListener(@Nullable ReaderEventListener eventListener) {
		this.eventListener = (eventListener != null ? eventListener : new EmptyReaderEventListener());
	}

	/**
	 * Specify the {@link SourceExtractor} to use.
	 * <p>The default implementation is {@link NullSourceExtractor} which simply returns {@code null}
	 * as the source object. This means that - during normal runtime execution -
	 * no additional source metadata is attached to the bean configuration metadata.
	 */
	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new NullSourceExtractor());
	}

	/**
	 * Specify the {@link NamespaceHandlerResolver} to use.
	 * <p>If none is specified, a default instance will be created through
	 * {@link #createDefaultNamespaceHandlerResolver()}.
	 */
	public void setNamespaceHandlerResolver(@Nullable NamespaceHandlerResolver namespaceHandlerResolver) {
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}

	/**
	 * Specify the {@link DocumentLoader} to use.
	 * <p>The default implementation is {@link DefaultDocumentLoader}
	 * which loads {@link Document} instances using JAXP.
	 */
	public void setDocumentLoader(@Nullable DocumentLoader documentLoader) {
		this.documentLoader = (documentLoader != null ? documentLoader : new DefaultDocumentLoader());
	}

	/**
	 * Set a SAX entity resolver to be used for parsing.
	 * <p>By default, {@link ResourceEntityResolver} will be used. Can be overridden
	 * for custom entity resolution, for example relative to some specific base path.
	 */
	public void setEntityResolver(@Nullable EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	/**
	 * Return the EntityResolver to use, building a default resolver
	 * if none specified.
	 * EntityResolver 作用就是通过实现它，应用可以自定义如何寻找 验证文件 的逻辑
	 *
	 * 在 loadDocument 方法中涉及一个参数 EntityResolver ，何为EntityResolver？
	 * 官网这样解释：如果 SAX 应用程序需要实现自定义处理外部实体，
	 * 则必须实现此接口并使用 setEntityResolver 方法向SAX 驱动器注册一个实例。
	 * 也就是说，对于解析一个XML，SAX 首先读取该 XML 文档上的声明，
	 * 根据声明去寻找相应的 DTD 定义，以便对文档进行一个验证。
	 * 默认的寻找规则，即通过网络（实现上就是声明的DTD的URI地址）来下载相应的DTD声明，并进行认证。
	 * 下载的过程是一个漫长的过程，而且当网络中断或不可用时，
	 * 这里会报错，就是因为相应的DTD声明没有被找到的原因。
	 *
	 * EntityResolver 的作用是项目本身就可以提供一个如何寻找 DTD 声明的方法，
	 * 即由程序来实现寻找 DTD 声明的过程，
	 * 比如我们将 DTD 文件放到项目中某处，
	 * 在实现时直接将此文档读取并返回给 SAX 即可。
	 * 这样就避免了通过网络来寻找相应的声明。
	 */
	protected EntityResolver getEntityResolver() {
		if (this.entityResolver == null) {
			// Determine default EntityResolver to use.
			ResourceLoader resourceLoader = getResourceLoader();
			if (resourceLoader != null) {
				this.entityResolver = new ResourceEntityResolver(resourceLoader);
			}
			else {
				// 创建一个 DelegatingEntityResolver 对象，该 Resolver 委托给默认的 BeansDtdResolver 和
				// PluggableSchemaResolver
				this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
			}
		}
		return this.entityResolver;
	}

	/**
	 * Set an implementation of the {@code org.xml.sax.ErrorHandler}
	 * interface for custom handling of XML parsing errors and warnings.
	 * <p>If not set, a default SimpleSaxErrorHandler is used that simply
	 * logs warnings using the logger instance of the view class,
	 * and rethrows errors to discontinue the XML transformation.
	 * @see SimpleSaxErrorHandler
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Specify the {@link BeanDefinitionDocumentReader} implementation to use,
	 * responsible for the actual reading of the XML bean definition document.
	 * <p>The default is {@link DefaultBeanDefinitionDocumentReader}.
	 * @param documentReaderClass the desired BeanDefinitionDocumentReader implementation class
	 */
	public void setDocumentReaderClass(Class<? extends BeanDefinitionDocumentReader> documentReaderClass) {
		this.documentReaderClass = documentReaderClass;
	}


	/**
	 *
	 * Load bean definitions from the specified XML file.
	 * @param resource the resource descriptor for the XML file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		// 将 resource 封装成 EncodedResource 主要是为了对 Resource 进行编码，保证内容读取的正确性
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * Load bean definitions from the specified XML file.
	 * @param encodedResource the resource descriptor for the XML file,
	 * allowing to specify an encoding to use for parsing the file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		Assert.notNull(encodedResource, "EncodedResource must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("Loading XML bean definitions from " + encodedResource);
		}

		// 获取已经加载过的资源
		Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
		if (currentResources == null) {
			currentResources = new HashSet<>(4);
			this.resourcesCurrentlyBeingLoaded.set(currentResources);
		}
		// 如果将资源已经被当前记录加载了，抛出异常
		// 为了避免 EncodedResource 在加载时候还没有加载完成的时候，又加载自身造成死循环
		// 也因此需要在 finally 处从缓存中踢出
		if (!currentResources.add(encodedResource)) {
			throw new BeanDefinitionStoreException(
					"Detected cyclic loading of " + encodedResource + " - check your import definitions!");
		}
		try {
			// 获取封装 Resource 的 InputStream
			InputStream inputStream = encodedResource.getResource().getInputStream();
			try {
				InputSource inputSource = new InputSource(inputStream);
				// 如果存在编码，则设置编码规则
				if (encodedResource.getEncoding() != null) {
					inputSource.setEncoding(encodedResource.getEncoding());
				}
				// 执行加载的核心逻辑
				return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"IOException parsing XML document from " + encodedResource.getResource(), ex);
		}
		finally {
			// 移除当前线程的这些缓存资源
			currentResources.remove(encodedResource);
			if (currentResources.isEmpty()) {
				this.resourcesCurrentlyBeingLoaded.remove();
			}
		}
	}

	/**
	 * Load bean definitions from the specified XML file.
	 * @param inputSource the SAX InputSource to read from
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(InputSource inputSource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(inputSource, "resource loaded through SAX InputSource");
	}

	/**
	 * Load bean definitions from the specified XML file.
	 * @param inputSource the SAX InputSource to read from
	 * @param resourceDescription a description of the resource
	 * (can be {@code null} or empty)
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(InputSource inputSource, @Nullable String resourceDescription)
			throws BeanDefinitionStoreException {

		return doLoadBeanDefinitions(inputSource, new DescriptiveResource(resourceDescription));
	}


	/**
	 * Actually load bean definitions from the specified XML file.
	 * @param inputSource the SAX InputSource to read from
	 * @param resource the resource descriptor for the XML file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 * @see #doLoadDocument
	 * @see #registerBeanDefinitions
	 */
	protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
			throws BeanDefinitionStoreException {

		try {
			// 获取一个 Document 实例
			Document doc = doLoadDocument(inputSource, resource);
			// 根据 Document 实例，注册 bean 信息，并且返回注册的数量
			int count = registerBeanDefinitions(doc, resource);
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + count + " bean definitions from " + resource);
			}
			return count;
		}
		catch (BeanDefinitionStoreException ex) {
			throw ex;
		}
		catch (SAXParseException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
		}
		catch (SAXException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"XML document from " + resource + " is invalid", ex);
		}
		catch (ParserConfigurationException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Parser configuration exception parsing XML from " + resource, ex);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"IOException parsing XML document from " + resource, ex);
		}
		catch (Throwable ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Unexpected exception parsing XML document from " + resource, ex);
		}
	}

	/**
	 * 为什么需要验证模式？
	 * XML 文件的验证模式保证了 XML 的正确性
	 *
	 * ---- DTD?
	 * DTD 为 xml 为文件验证机制，属于 xml 文件的一部分。是一种保证 XML 文档格式正确的有效验证方式，
	 * 它定义了相关 XML 文档的元素、属性、排列方式、元素内容以及元素的层次结构，
	 * 我们可以通过比较 XML 和 DTD 文件来看文档是否符合规范，元素和标签是否使用正确。
	 * 要再 spring 中使用 DTD，需要在 XML 中文件头部声明
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <!DOCTYPE beans PUBLIC  "-//SPRING//DTD BEAN//EN"  "http://www.springframework.org/dtd/spring-beans.dtd">
	 *
	 * ---- DTD 缺陷
	 * 1. 没有使用 XML 格式，而是自己定义了一套格式，相对解析器重用性较差; 而且 DTD 的构建和访问
	 * 没有标准的编程接口，因而解析器很难解析 DTD 文档。
	 * 2. DTD 对元素类型限制较少，同事其它的约束力也弱。
	 * 3. DTD 拓展能力较差
	 * 4. 基于正则表达式的 DTD 文档的描述能力有限
	 *
	 * ----- XSD?
	 * 针对 DTD 的缺陷，W3C 在 2001 年推出 XSD 即 XML Schema 语言。它本身就是一个 XML 文档，使用的是 xml 语法
	 * 因此可以很方便的解析 XSD 文档。 XSD 具有如下优势
	 * 1. XML Schema 基于 XML，没有专门的语法
	 * 2. XML Schema 可以像其它 XML 文件一样解析和处理。
	 * 3. XML Schema 比 DTD 提供了更丰富的数据类型。
	 * 4. 提供可扩充的数据模型。
	 * 5. 支持综合命名空间。
	 * 6. 支持属性组
	 * Actually load the specified document using the configured DocumentLoader.
	 * @param inputSource the SAX InputSource to read from
	 * @param resource the resource descriptor for the XML file
	 * @return the DOM Document
	 * @throws Exception when thrown from the DocumentLoader
	 * @see #setDocumentLoader
	 * @see DocumentLoader#loadDocument
	 */
	protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
		// getValidationModeForResource 获取指定资源（xml）的验证模式
		// loadDocument 获取 XML Document 实例
		return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler,
				getValidationModeForResource(resource), isNamespaceAware());
	}

	/**
	 * Determine the validation mode for the specified {@link Resource}.
	 * If no explicit validation mode has been configured, then the validation
	 * mode gets {@link #detectValidationMode detected} from the given resource.
	 * <p>Override this method if you would like full control over the validation
	 * mode, even when something other than {@link #VALIDATION_AUTO} was set.
	 * @see #detectValidationMode
	 */
	protected int getValidationModeForResource(Resource resource) {
		// 获取验证模式
		int validationModeToUse = getValidationMode();
		// 如果手动指定了则直接返回
		if (validationModeToUse != VALIDATION_AUTO) {
			return validationModeToUse;
		}
		// 自动获取验证模式
		int detectedMode = detectValidationMode(resource);
		if (detectedMode != VALIDATION_AUTO) {
			return detectedMode;
		}
		// 最后返回默认 XSD 验证模式
		// Hmm, we didn't get a clear indication... Let's assume XSD,
		// since apparently no DTD declaration has been found up until
		// detection stopped (before finding the document's root tag).
		return VALIDATION_XSD;
	}

	/**
	 * Detect which kind of validation to perform on the XML file identified
	 * by the supplied {@link Resource}. If the file has a {@code DOCTYPE}
	 * definition then DTD validation is used otherwise XSD validation is assumed.
	 * <p>Override this method if you would like to customize resolution
	 * of the {@link #VALIDATION_AUTO} mode.
	 */
	protected int detectValidationMode(Resource resource) {
		// 不可读，抛出异常
		if (resource.isOpen()) {
			throw new BeanDefinitionStoreException(
					"Passed-in Resource [" + resource + "] contains an open stream: " +
					"cannot determine validation mode automatically. Either pass in a Resource " +
					"that is able to create fresh streams, or explicitly specify the validationMode " +
					"on your XmlBeanDefinitionReader instance.");
		}

		InputStream inputStream;
		try {
			inputStream = resource.getInputStream();
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"Unable to determine validation mode for [" + resource + "]: cannot open InputStream. " +
					"Did you attempt to load directly from a SAX InputSource without specifying the " +
					"validationMode on your XmlBeanDefinitionReader instance?", ex);
		}

		try {
			// 获取对应的验证模式
			return this.validationModeDetector.detectValidationMode(inputStream);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("Unable to determine validation mode for [" +
					resource + "]: an error occurred whilst reading from the InputStream.", ex);
		}
	}

	/**
	 * Register the bean definitions contained in the given DOM document.
	 * Called by {@code loadBeanDefinitions}.
	 * <p>Creates a new instance of the parser class and invokes
	 * {@code registerBeanDefinitions} on it.
	 * @param doc the DOM document
	 * @param resource the resource descriptor (for context information)
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of parsing errors
	 * @see #loadBeanDefinitions
	 * @see #setDocumentReaderClass
	 * @see BeanDefinitionDocumentReader#registerBeanDefinitions
	 */
	public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
		// 创建 BeanDefinitionDocumentReader 对象，定义读取 Document 并注册 BeanDefinition 的功能
		BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
		// 获取已经注册 BeanDefinition 数量
		int countBefore = getRegistry().getBeanDefinitionCount();
		// 创建 XmlReaderContext 对象，读取 xml 并
		// 注册 BeanDefinition
		documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
		// 计算新注册的 BeanDefinition 的数量
		return getRegistry().getBeanDefinitionCount() - countBefore;
	}

	/**
	 * Create the {@link BeanDefinitionDocumentReader} to use for actually
	 * reading bean definitions from an XML document.
	 * <p>The default implementation instantiates the specified "documentReaderClass".
	 * @see #setDocumentReaderClass
	 */
	protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
		return BeanUtils.instantiateClass(this.documentReaderClass);
	}

	/**
	 * Create the {@link XmlReaderContext} to pass over to the document reader.
	 */
	public XmlReaderContext createReaderContext(Resource resource) {
		return new XmlReaderContext(resource, this.problemReporter, this.eventListener,
				this.sourceExtractor, this, getNamespaceHandlerResolver());
	}

	/**
	 * Lazily create a default NamespaceHandlerResolver, if not set before.
	 * @see #createDefaultNamespaceHandlerResolver()
	 */
	public NamespaceHandlerResolver getNamespaceHandlerResolver() {
		if (this.namespaceHandlerResolver == null) {
			this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
		}
		return this.namespaceHandlerResolver;
	}

	/**
	 * Create the default implementation of {@link NamespaceHandlerResolver} used if none is specified.
	 * <p>The default implementation returns an instance of {@link DefaultNamespaceHandlerResolver}.
	 * @see DefaultNamespaceHandlerResolver#DefaultNamespaceHandlerResolver(ClassLoader)
	 */
	protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
		ClassLoader cl = (getResourceLoader() != null ? getResourceLoader().getClassLoader() : getBeanClassLoader());
		return new DefaultNamespaceHandlerResolver(cl);
	}

}
