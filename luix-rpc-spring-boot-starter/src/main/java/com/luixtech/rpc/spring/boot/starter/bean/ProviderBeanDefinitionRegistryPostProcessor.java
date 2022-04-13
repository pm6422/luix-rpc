package com.luixtech.rpc.spring.boot.starter.bean;

import com.luixtech.rpc.spring.boot.starter.bean.name.DefaultBeanNameGenerator;
import com.luixtech.rpc.spring.boot.starter.bean.registry.ClassPathBeanDefinitionRegistryScanner;
import com.luixtech.rpc.spring.boot.starter.utils.AnnotationBeanDefinitionUtils;
import com.luixtech.rpc.spring.boot.starter.utils.AnnotationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.rpc.core.config.impl.ProtocolConfig;
import com.luixtech.rpc.core.config.impl.ProviderConfig;
import com.luixtech.rpc.core.server.annotation.RpcProvider;
import com.luixtech.rpc.core.server.stub.ProviderStub;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.luixtech.rpc.core.constant.ServiceConstants.*;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static com.luixtech.rpc.core.constant.ProtocolConstants.PROTOCOL;
import static com.luixtech.rpc.core.constant.ProtocolConstants.SERIALIZER;
import static com.luixtech.rpc.core.constant.ProviderConstants.HEALTH_CHECKER;
import static com.luixtech.rpc.core.server.stub.ProviderStub.buildProviderStubBeanName;
import static com.luixtech.rpc.spring.boot.starter.config.LuixProperties.readProtocolConfig;
import static com.luixtech.rpc.spring.boot.starter.config.LuixProperties.readProviderConfig;
import static com.luixtech.rpc.spring.boot.starter.utils.AnnotationBeanDefinitionUtils.addPropertyValue;

/**
 * Register provider bean and provider stub under specified scan base packages to spring context
 * by {@link BeanDefinitionRegistry}
 * <p>
 * BeanFactoryPostProcessor: Factory hook that allows for custom modification of an application context's
 * bean definitions and the bean property values of the context's underlying bean factory.
 * BeanDefinitionRegistryPostProcessor: It's the sub-interface of
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor},
 * it can register the beanDefinition to beanFactory before
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor} takes effect
 */
@Slf4j
public class ProviderBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor,
        EnvironmentAware, BeanFactoryAware, ResourceLoaderAware, BeanClassLoaderAware {

    private final List<String>               scanBasePackages;
    private       ConfigurableEnvironment    env;
    private       ResourceLoader             resourceLoader;
    private       ClassLoader                classLoader;
    /**
     * {@link DefaultListableBeanFactory} can register bean definition
     */
    private       DefaultListableBeanFactory beanFactory;

    public ProviderBeanDefinitionRegistryPostProcessor(List<String> scanBasePackages) {
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
        this.env = (ConfigurableEnvironment) environment;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(DefaultListableBeanFactory.class, beanFactory,
                "It requires an instance of ".concat(DefaultListableBeanFactory.class.getSimpleName()));
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Modify the application context's internal bean definition registry after its
     * standard initialization. All regular bean definitions will have been loaded,
     * but no beans will have been instantiated yet. This allows for adding further
     * bean definitions before the next post-processing phase kicks in.
     *
     * @param registry the bean definition registry used by the application context
     * @throws org.springframework.beans.BeansException in case of errors
     */
    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
        // Register provider and provider stub beans
        registerProviderBeans(registry);
    }

    /**
     * Register provider and provider stub beans
     *
     * @param registry current bean definition registry
     */
    private void registerProviderBeans(BeanDefinitionRegistry registry) {
        List<String> resolvedScanBasePackages = resolvePackagePlaceholders();
        doRegisterProviderBeans(registry, resolvedScanBasePackages);
    }

    /**
     * Resolve the placeholder in package name
     *
     * @return replaced packages
     */
    private List<String> resolvePackagePlaceholders() {
        return scanBasePackages
                .stream()
                .map(x -> env.resolvePlaceholders(x))
                .collect(Collectors.toList());
    }

    /**
     * Register provider and provider stub beans
     *
     * @param registry                 current bean definition registry
     * @param resolvedScanBasePackages provider packages to be scanned
     */
    private void doRegisterProviderBeans(BeanDefinitionRegistry registry, List<String> resolvedScanBasePackages) {
        BeanNameGenerator beanNameGenerator = DefaultBeanNameGenerator.create();
        ClassPathBeanDefinitionRegistryScanner providerScanner = createProviderScanner(registry, beanNameGenerator);

        resolvedScanBasePackages.forEach(scanBasePackage -> {
            // Register provider stub first
            boolean registered = registerProviderStubBeans(registry, beanNameGenerator, providerScanner, scanBasePackage);
            if (registered) {
                // Then register provider beans
                registerProviderBeans(providerScanner, scanBasePackage);
                log.info("Registered all RPC provider bean instances to spring context");
            }
        });
    }

    /**
     * Create provider registry scanner which can found the below service
     *
     * @param registry          current bean definition registry
     * @param beanNameGenerator bean name generator
     * @return bean definition registry scanner
     *
     * <code>
     * '@RpcProvider(retryCount="1")
     * public class AppServiceImpl {
     * ...
     * ...
     * }'
     * </code>
     */
    private ClassPathBeanDefinitionRegistryScanner createProviderScanner(BeanDefinitionRegistry registry,
                                                                         BeanNameGenerator beanNameGenerator) {
        ClassPathBeanDefinitionRegistryScanner scanner =
                new ClassPathBeanDefinitionRegistryScanner(registry, env, resourceLoader);
        scanner.setBeanNameGenerator(beanNameGenerator);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RpcProvider.class));
        return scanner;
    }

    /**
     * Register provider beans with {@link RpcProvider} annotation
     *
     * @param providerScanner provider bean definition registry scanner
     * @param scanBasePackage provider packages to be scanned
     */
    private void registerProviderBeans(ClassPathBeanDefinitionRegistryScanner providerScanner, String scanBasePackage) {
        // The 'scan' method can register @Provider bean instance to spring context
        providerScanner.scan(scanBasePackage);
    }

    /**
     * Register provider stub {@link ProviderStub} beans
     *
     * @param registry          current bean definition registry
     * @param beanNameGenerator bean name generator
     * @param providerScanner   provider bean definition registry scanner
     * @param scanBasePackage   provider packages to be scanned
     * @return {@code true} if it was registered and {@code false} otherwise
     */
    private boolean registerProviderStubBeans(BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator,
                                              ClassPathBeanDefinitionRegistryScanner providerScanner, String scanBasePackage) {
        // Next we need to register ProviderBean which is the stub of service provider to spring context
        Set<BeanDefinitionHolder> holders =
                findProviderBeanDefinitionHolders(providerScanner, scanBasePackage, registry, beanNameGenerator);
        if (CollectionUtils.isEmpty(holders)) {
            return false;
        }
        holders.forEach(holder -> registerProviderStubBean(holder, registry, providerScanner));
        return true;
    }

    /**
     * Find already registered provider bean definitions
     *
     * @param providerScanner   provider bean definition registry scanner
     * @param scanBasePackage   provider packages to be scanned
     * @param registry          current bean definition registry
     * @param beanNameGenerator bean name generator
     * @return provider bean definition holders
     */
    private Set<BeanDefinitionHolder> findProviderBeanDefinitionHolders(ClassPathBeanDefinitionRegistryScanner providerScanner,
                                                                        String scanBasePackage,
                                                                        BeanDefinitionRegistry registry,
                                                                        BeanNameGenerator beanNameGenerator) {
        // Find the provider components satisfying the condition
        Set<BeanDefinition> beanDefinitions = providerScanner.findCandidateComponents(scanBasePackage);
        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<>(beanDefinitions.size());
        beanDefinitions.forEach(beanDefinition -> {
            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            beanDefinitionHolders.add(new BeanDefinitionHolder(beanDefinition, beanName));
        });
        return beanDefinitionHolders;
    }

    /**
     * Register {@link ProviderStub} beans
     *
     * @param providerBeanDefinitionHolder provider bean definition holders
     * @param registry                     current bean definition registry
     * @param providerScanner              provider bean definition registry scanner
     */
    private void registerProviderStubBean(BeanDefinitionHolder providerBeanDefinitionHolder,
                                          BeanDefinitionRegistry registry,
                                          ClassPathBeanDefinitionRegistryScanner providerScanner) {
        Class<?> providerInstanceClass = resolveProviderClass(providerBeanDefinitionHolder);
        RpcProvider rpcProviderAnnotation = findProviderAnnotation(providerInstanceClass);
        Class<?> providerInterfaceClass = resolveProviderInterface(providerInstanceClass);

        String providerStubBeanName = buildProviderStubBeanName(providerInterfaceClass.getName(),
                rpcProviderAnnotation.form(), rpcProviderAnnotation.version());
        AbstractBeanDefinition stubBeanDefinition = buildProviderStubDefinition(providerStubBeanName,
                providerInterfaceClass, rpcProviderAnnotation, providerBeanDefinitionHolder.getBeanName());

        // Check duplicated candidate bean
        if (providerScanner.checkCandidate(providerStubBeanName, stubBeanDefinition)) {
            registry.registerBeanDefinition(providerStubBeanName, stubBeanDefinition);
            log.info("Registered RPC provider stub [{}] to spring context", providerStubBeanName);
        }
    }

    /**
     * Create provider class
     *
     * @param beanDefinitionHolder provider bean definition holders
     * @return provider class
     */
    private Class<?> resolveProviderClass(BeanDefinitionHolder beanDefinitionHolder) {
        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        String beanClassName = beanDefinition.getBeanClassName();
        return ClassUtils.resolveClassName(Objects.requireNonNull(beanClassName), classLoader);
    }

    /**
     * Get {@link RpcProvider} annotation
     *
     * @param beanClass provider bean class
     * @return {@link RpcProvider} annotation
     */
    private RpcProvider findProviderAnnotation(Class<?> beanClass) {
        return beanClass.getAnnotation(RpcProvider.class);
    }

    /**
     * Get provider interface class
     *
     * @param providerInstanceClass provider instance class, e.g AppServiceImpl
     * @return provider interface
     */
    private Class<?> resolveProviderInterface(Class<?> providerInstanceClass) {
        AnnotationAttributes annotationAttributes = AnnotationUtils
                .getAnnotationAttributes(providerInstanceClass, RpcProvider.class, env, false, true);
        return AnnotationUtils.resolveInterfaceClass(annotationAttributes, providerInstanceClass);
    }

    /**
     * Build {@link ProviderStub} definition
     *
     * @param beanName             provider stub bean name
     * @param interfaceClass       provider interface class
     * @param annotation           {@link RpcProvider} annotation
     * @param providerInstanceName provider instance name
     * @return {@link ProviderStub} bean definition
     */
    private AbstractBeanDefinition buildProviderStubDefinition(String beanName,
                                                               Class<?> interfaceClass,
                                                               RpcProvider annotation,
                                                               String providerInstanceName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ProviderStub.class);
        ProtocolConfig protocolConfig = readProtocolConfig(env);
        ProviderConfig providerConfig = readProviderConfig(env);

        AnnotationBeanDefinitionUtils.addPropertyValue(builder, BEAN_NAME, beanName);
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, INTERFACE_CLASS, interfaceClass);
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, INTERFACE_NAME, interfaceClass.getName());

        String protocol = defaultIfEmpty(annotation.protocol(), protocolConfig.getName());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, PROTOCOL, protocol);

        String serializer = defaultIfEmpty(annotation.serializer(), protocolConfig.getSerializer());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, SERIALIZER, serializer);

        String form = defaultIfEmpty(annotation.form(), providerConfig.getForm());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, FORM, form);

        String version = defaultIfEmpty(annotation.version(), providerConfig.getVersion());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, VERSION, version);

        String healthChecker = defaultIfEmpty(annotation.healthChecker(), providerConfig.getHealthChecker());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, HEALTH_CHECKER, healthChecker);

        Integer requestTimeout = StringUtils.isEmpty(annotation.requestTimeout())
                ? providerConfig.getRequestTimeout() : Integer.valueOf(annotation.requestTimeout());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, REQUEST_TIMEOUT, requestTimeout);

        Integer retryCount = StringUtils.isEmpty(annotation.retryCount())
                ? providerConfig.getRetryCount() : Integer.valueOf(annotation.retryCount());
        AnnotationBeanDefinitionUtils.addPropertyValue(builder, RETRY_COUNT, retryCount);

        AnnotationBeanDefinitionUtils.addPropertyValue(builder, MAX_PAYLOAD, providerConfig.getMaxPayload());

        // Obtain the instance by instance name then assign it to the property
        AnnotationBeanDefinitionUtils.addPropertyReference(builder, "instance", providerInstanceName, env);
        return builder.getBeanDefinition();
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // Leave blank intentionally
    }
}