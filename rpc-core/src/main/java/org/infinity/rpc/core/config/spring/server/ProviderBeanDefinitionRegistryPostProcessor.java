package org.infinity.rpc.core.config.spring.server;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.bean.DefaultBeanNameGenerator;
import org.infinity.rpc.core.config.spring.bean.registry.ClassPathBeanDefinitionRegistryScanner;
import org.infinity.rpc.core.config.spring.server.providerwrapper.ProviderWrapper;
import org.infinity.rpc.core.config.spring.server.providerwrapper.ProviderWrapperBeanNameBuilder;
import org.infinity.rpc.core.config.spring.utils.AnnotationUtils;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.server.annotation.Provider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Register provider bean and provider wrapper under specified scan base packages to spring context
 * by {@link BeanDefinitionRegistry}
 */
@Slf4j
public class ProviderBeanDefinitionRegistryPostProcessor implements EnvironmentAware, ResourceLoaderAware,
        BeanClassLoaderAware, BeanDefinitionRegistryPostProcessor {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final        Set<String>      scanBasePackages;
    private              Environment      env;
    private              ResourceLoader   resourceLoader;
    private              ClassLoader      classLoader;

    public ProviderBeanDefinitionRegistryPostProcessor(Set<String> scanBasePackages) {
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setEnvironment(@Nonnull Environment environment) {
        this.env = environment;
    }

    @Override
    public void setResourceLoader(@Nonnull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(@Nonnull ClassLoader classLoader) {
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
    public void postProcessBeanDefinitionRegistry(@Nonnull BeanDefinitionRegistry registry) throws BeansException {
        registerProviderBeans(registry, scanBasePackages);
    }

    /**
     * Register provider beans
     *
     * @param registry         current bean definition registry
     * @param scanBasePackages provider packages to be scanned
     */
    private void registerProviderBeans(BeanDefinitionRegistry registry, Set<String> scanBasePackages) {
        Set<String> resolvedScanBasePackages = resolvePackagePlaceholders(scanBasePackages);
        if (CollectionUtils.isEmpty(resolvedScanBasePackages)) {
            log.warn("No package to be scanned for registering providers!");
            return;
        }
        registerProviders(registry, resolvedScanBasePackages);
    }

    /**
     * Resolve the placeholder in package name
     *
     * @param scanBasePackages packages to be scanned
     * @return replaced packages
     */
    private Set<String> resolvePackagePlaceholders(Set<String> scanBasePackages) {
        return scanBasePackages
                .stream()
                .filter(StringUtils::hasText)
                .map(x -> env.resolvePlaceholders(x.trim()))
                .collect(Collectors.toSet());
    }

    /**
     * Register provider and provider wrapper beans
     *
     * @param registry                 current bean definition registry
     * @param resolvedScanBasePackages provider packages to be scanned
     */
    private void registerProviders(BeanDefinitionRegistry registry, Set<String> resolvedScanBasePackages) {
        BeanNameGenerator beanNameGenerator = DefaultBeanNameGenerator.create();
        ClassPathBeanDefinitionRegistryScanner providerScanner = createProviderScanner(registry, beanNameGenerator);

        resolvedScanBasePackages.forEach(scanBasePackage -> {
            // Register provider beans first
            registerProviderBeans(providerScanner, scanBasePackage);
            // Then register provider wrapper beans
            registerProviderWrapperBeans(registry, beanNameGenerator, providerScanner, scanBasePackage);
        });
    }

    /**
     * Create provider registry scanner
     *
     * @param registry          current bean definition registry
     * @param beanNameGenerator bean name generator
     * @return bean definition registry scanner
     */
    private ClassPathBeanDefinitionRegistryScanner createProviderScanner(BeanDefinitionRegistry registry,
                                                                         BeanNameGenerator beanNameGenerator) {
        ClassPathBeanDefinitionRegistryScanner scanner = new ClassPathBeanDefinitionRegistryScanner(registry, env, resourceLoader);
        scanner.setBeanNameGenerator(beanNameGenerator);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Provider.class));
        return scanner;
    }

    /**
     * Register provider beans with {@link Provider} annotation
     *
     * @param providerScanner provider bean definition registry scanner
     * @param scanBasePackage provider packages to be scanned
     */
    private void registerProviderBeans(ClassPathBeanDefinitionRegistryScanner providerScanner, String scanBasePackage) {
        // The 'scan' method can register @Provider bean instance to spring context
        providerScanner.scan(scanBasePackage);
        log.info("Registered RPC provider instances to spring context");
    }

    /**
     * Register provider wrapper {@link ProviderWrapper} beans
     *
     * @param registry          current bean definition registry
     * @param beanNameGenerator bean name generator
     * @param providerScanner   provider bean definition registry scanner
     * @param scanBasePackage   provider packages to be scanned
     */
    private void registerProviderWrapperBeans(BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator,
                                              ClassPathBeanDefinitionRegistryScanner providerScanner, String scanBasePackage) {
        // Next we need to register ProviderBean which is the wrapper of service provider to spring context
        Set<BeanDefinitionHolder> holders =
                findProviderBeanDefinitionHolders(providerScanner, scanBasePackage, registry, beanNameGenerator);
        if (CollectionUtils.isEmpty(holders)) {
            return;
        }
        holders.forEach(holder -> registerProviderWrapperBean(holder, registry, providerScanner));
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
     * Register {@link ProviderWrapper} beans
     *
     * @param providerBeanDefinitionHolder provider bean definition holders
     * @param registry                     current bean definition registry
     * @param providerScanner              provider bean definition registry scanner
     */
    private void registerProviderWrapperBean(BeanDefinitionHolder providerBeanDefinitionHolder,
                                             BeanDefinitionRegistry registry,
                                             ClassPathBeanDefinitionRegistryScanner providerScanner) {
        Class<?> providerInstanceClass = resolveProviderClass(providerBeanDefinitionHolder);
        Provider providerAnnotation = findProviderAnnotation(providerInstanceClass);
        Class<?> providerInterfaceClass = resolveProviderInterface(providerAnnotation, providerInstanceClass);

        String providerWrapperBeanName = buildProviderWrapperBeanName(providerInterfaceClass);
        AbstractBeanDefinition wrapperBeanDefinition = buildProviderWrapperDefinition(
                providerInterfaceClass, providerAnnotation, providerBeanDefinitionHolder.getBeanName());

        // Check duplicated candidate bean
        if (providerScanner.checkCandidate(providerWrapperBeanName, wrapperBeanDefinition)) {
            registry.registerBeanDefinition(providerWrapperBeanName, wrapperBeanDefinition);
            log.info("Registered RPC provider wrapper for [{}] to spring context", providerWrapperBeanName);
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
     * Get {@link Provider} annotation
     *
     * @param beanClass provider bean class
     * @return {@link Provider} annotation
     */
    private Provider findProviderAnnotation(Class<?> beanClass) {
        return beanClass.getAnnotation(Provider.class);
    }

    /**
     * Get provider interface class
     *
     * @param providerAnnotation    {@link Provider} annotation
     * @param providerInstanceClass provider instance class, e.g AppServiceImpl
     * @return provider interface
     */
    private Class<?> resolveProviderInterface(Provider providerAnnotation, Class<?> providerInstanceClass) {
        AnnotationAttributes annotationAttributes = AnnotationUtils
                .getAnnotationAttributes(providerInstanceClass, Provider.class, env, false, true);
        return AnnotationUtils.resolveInterfaceClass(annotationAttributes, providerInstanceClass);
    }

    /**
     * Build provider wrapper bean name
     *
     * @param interfaceClass provider interface class
     * @return provider wrapper bean name
     */
    private String buildProviderWrapperBeanName(Class<?> interfaceClass) {
        return ProviderWrapperBeanNameBuilder.builder(interfaceClass, env).build();
    }

    /**
     * Build {@link ProviderWrapper} definition
     *
     * @param providerInterfaceClass provider interface class
     * @param providerAnnotation     {@link Provider} annotation
     * @param providerInstanceName   provider instance name
     * @return {@link ProviderWrapper} bean definition
     */
    private AbstractBeanDefinition buildProviderWrapperDefinition(Class<?> providerInterfaceClass,
                                                                  Provider providerAnnotation,
                                                                  String providerInstanceName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ProviderWrapper.class);
        addPropertyValue(builder, "interfaceName", providerInterfaceClass.getName(), ProviderWrapper.class, false);
        addPropertyValue(builder, "interfaceClass", providerInterfaceClass, ProviderWrapper.class, false);
        addPropertyValue(builder, "instanceName", providerInstanceName, ProviderWrapper.class, false);
        addPropertyValue(builder, "maxRetries", providerAnnotation.maxRetries(), ProviderWrapper.class, true);
        addPropertyValue(builder, "checkHealth", providerAnnotation.checkHealth(), ProviderWrapper.class, false);
        // Obtain the instance by instance name then assign it to the property
        addPropertyReference(builder, "instance", providerInstanceName);

        return builder.getBeanDefinition();
    }

    private void addPropertyValue(BeanDefinitionBuilder builder, String propertyName,
                                  Object propertyValue, Class<?> beanType, boolean validate) {
        if (validate) {
            validatePropertyValue(beanType, propertyName, propertyValue);
        }
        builder.addPropertyValue(propertyName, propertyValue);
    }

    private void validatePropertyValue(Class<?> beanType, String propertyName, Object propertyValue) {
        try {
            List<String> messages = doValidate(beanType, propertyName, propertyValue);
            Assert.isTrue(CollectionUtils.isEmpty(messages), String.join(",", messages));
        } catch (Exception e) {
            // Re-throw the exception
            throw new RpcConfigurationException(e.getMessage());
        }
    }

    private static <T> List<String> doValidate(Class<T> beanType, String propertyName, Object propertyValue) {
        Set<ConstraintViolation<T>> constraintViolations = VALIDATOR_FACTORY.getValidator()
                .validateValue(beanType, propertyName, propertyValue);
        return constraintViolations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
    }

    /**
     * Obtain the instance by instance name then assign it to the property
     *
     * @param builder      bean definition builder
     * @param propertyName property name
     * @param instanceName provider instance name
     */
    private void addPropertyReference(BeanDefinitionBuilder builder, String propertyName, String instanceName) {
        builder.addPropertyReference(propertyName, env.resolvePlaceholders(instanceName));
    }

    @Override
    public void postProcessBeanFactory(@Nonnull ConfigurableListableBeanFactory arg) throws BeansException {
        // Leave blank intentionally
    }
}