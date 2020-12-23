package org.infinity.rpc.core.config.spring.server;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.bean.DefaultBeanNameGenerator;
import org.infinity.rpc.core.config.spring.bean.registry.ClassPathBeanDefinitionRegistryScanner;
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
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Register provider bean and provider wrapper to spring context.
 */
@Slf4j
public class ProviderBeanDefinitionRegistryPostProcessor implements EnvironmentAware, ResourceLoaderAware,
        BeanClassLoaderAware, BeanDefinitionRegistryPostProcessor {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final        Set<String>      scanBasePackages;
    private              Environment      environment;
    private              ResourceLoader   resourceLoader;
    private              ClassLoader      classLoader;

    public ProviderBeanDefinitionRegistryPostProcessor(Set<String> scanBasePackages) {
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setEnvironment(@Nonnull Environment environment) {
        this.environment = environment;
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
                .map(x -> environment.resolvePlaceholders(x.trim()))
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
            // Register provider first
            registerProviderBeans(providerScanner, scanBasePackage);
            // Then register provider wrapper
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
        ClassPathBeanDefinitionRegistryScanner scanner = new ClassPathBeanDefinitionRegistryScanner(registry, environment, resourceLoader);
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
        Class<?> providerBeanClass = resolveProviderClass(providerBeanDefinitionHolder);
        Provider providerAnnotation = findProviderAnnotation(providerBeanClass);
        Class<?> providerInterfaceClass = findProviderInterface(providerAnnotation, providerBeanClass);

        String providerWrapperBeanName = buildProviderWrapperBeanName(providerInterfaceClass);
        AbstractBeanDefinition wrapperBeanDefinition = buildProviderWrapperDefinition(
                providerInterfaceClass, providerAnnotation, providerBeanDefinitionHolder.getBeanName());

        // Check duplicated candidate bean
        if (providerScanner.checkCandidate(providerWrapperBeanName, wrapperBeanDefinition)) {
            registry.registerBeanDefinition(providerWrapperBeanName, wrapperBeanDefinition);
            log.info("Registered RPC provider wrapper [{}] to spring context", providerWrapperBeanName);
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
     * @param providerAnnotation {@link Provider} annotation
     * @param providerBeanClass  provider class
     * @return provider interface
     */
    private Class<?> findProviderInterface(Provider providerAnnotation, Class<?> providerBeanClass) {
        Class<?>[] interfaceClasses = providerBeanClass.getInterfaces();
        Class<?> interfaceClass;

        if (interfaceClasses.length == 0) {
            throw new RpcConfigurationException("The RPC service provider bean must implement more than one interfaces!");
        } else if (interfaceClasses.length == 1) {
            interfaceClass = interfaceClasses[0];
        } else {
            // Get service interface from annotation if a instance has more than one declared interfaces
            interfaceClass = providerAnnotation.interfaceClass();
            if (void.class.equals(providerAnnotation.interfaceClass())) {
                throw new RpcConfigurationException("The @Provider annotation of RPC service provider must specify interfaceClass attribute value " +
                        "if the bean implements more than one interfaces!");
            }
            //  TODO: interfaceName handle
        }
        return interfaceClass;
    }

    /**
     * Build provider wrapper bean name
     *
     * @param interfaceClass provider interface class
     * @return provider wrapper bean name
     */
    private String buildProviderWrapperBeanName(Class<?> interfaceClass) {
        return ProviderWrapperBeanNameBuilder.builder(interfaceClass, environment).build();
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

    private void addPropertyValue(BeanDefinitionBuilder builder, String propertyName, Object propertyValue, Class<?> providerWrapperClass, boolean validate) {
        if (validate) {
            validatePropertyValue(providerWrapperClass, propertyName, propertyValue);
        }
        builder.addPropertyValue(propertyName, propertyValue);
    }

    private void validatePropertyValue(Class<?> providerWrapperClass, String propertyName, Object propertyValue) {
        try {
            List<String> messages = doValidate(providerWrapperClass, propertyName, propertyValue);
            if (!CollectionUtils.isEmpty(messages)) {
                for (String message : messages) {
                    throw new RuntimeException(message);
                }
            }
        } catch (Exception e) {
            // Re-throw the exception
            throw new RpcConfigurationException(e.getMessage());
        }
    }

    private static <T> List<String> doValidate(Class<T> beanType, String propertyName, Object propertyValue) {
        Validator validator = VALIDATOR_FACTORY.getValidator();
        Set<ConstraintViolation<T>> constraintViolations = validator.validateValue(beanType, propertyName, propertyValue);

        List<String> messageList = new ArrayList<>();
        for (ConstraintViolation<T> constraintViolation : constraintViolations) {
            messageList.add(constraintViolation.getMessage());
        }
        return messageList;
    }

    /**
     * Obtain the instance by instance name then assign it to the property
     *
     * @param builder      bean definition builder
     * @param propertyName property name
     * @param instanceName provider instance name
     */
    private void addPropertyReference(BeanDefinitionBuilder builder, String propertyName, String instanceName) {
        builder.addPropertyReference(propertyName, environment.resolvePlaceholders(instanceName));
    }

    @Override
    public void postProcessBeanFactory(@Nonnull ConfigurableListableBeanFactory arg) throws BeansException {
        // Leave blank intentionally
    }
}