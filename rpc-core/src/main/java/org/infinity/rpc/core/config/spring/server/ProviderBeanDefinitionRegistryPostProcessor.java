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

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Register provider bean and provider wrapper to spring context.
 */
@Slf4j
public class ProviderBeanDefinitionRegistryPostProcessor implements EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware, BeanDefinitionRegistryPostProcessor {

    private              Set<String>      scanBasePackages;
    private              Environment      environment;
    private              ResourceLoader   resourceLoader;
    private              ClassLoader      classLoader;
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    public ProviderBeanDefinitionRegistryPostProcessor(Set<String> scanBasePackages) {
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        registerBeans(registry, scanBasePackages);
    }

    private void registerBeans(BeanDefinitionRegistry registry, Set<String> scanBasePackages) {
        Set<String> resolvedScanBasePackages = resolvePackagePlaceholders(scanBasePackages);
        if (CollectionUtils.isEmpty(resolvedScanBasePackages)) {
            log.warn("No scan package to register providers!");
            return;
        }
        registerProviders(registry, resolvedScanBasePackages);
    }

    private Set<String> resolvePackagePlaceholders(Set<String> scanBasePackages) {
        return scanBasePackages
                .stream()
                .filter(x -> StringUtils.hasText(x))
                .map(x -> environment.resolvePlaceholders(x.trim()))
                .collect(Collectors.toSet());
    }

    private void registerProviders(BeanDefinitionRegistry registry, Set<String> resolvedScanBasePackages) {
        BeanNameGenerator beanNameGenerator = DefaultBeanNameGenerator.create();
        ClassPathBeanDefinitionRegistryScanner providerScanner = createProviderScanner(registry, beanNameGenerator);

        resolvedScanBasePackages.forEach(scanBasePackage -> {
            // Register provider first
            registerProviderInstances(providerScanner, scanBasePackage);
            // Then register provider wrapper
            registerProviderWrapperBeans(registry, beanNameGenerator, providerScanner, scanBasePackage);
        });
    }

    private ClassPathBeanDefinitionRegistryScanner createProviderScanner(BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator) {
        ClassPathBeanDefinitionRegistryScanner scanner = new ClassPathBeanDefinitionRegistryScanner(registry, environment, resourceLoader);
        scanner.setBeanNameGenerator(beanNameGenerator);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Provider.class));
        return scanner;
    }

    private void registerProviderInstances(ClassPathBeanDefinitionRegistryScanner providerScanner, String scanBasePackage) {
        // The 'scan' method can register @Provider bean instance to spring context
        providerScanner.scan(scanBasePackage);
        log.info("Registered RPC provider instances to spring context");
    }

    private void registerProviderWrapperBeans(BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator, ClassPathBeanDefinitionRegistryScanner providerScanner, String scanBasePackage) {
        // Next we need to register ProviderBean which is the wrapper of service provider to spring context
        Set<BeanDefinitionHolder> providerBeanDefinitionHolders = findProviderBeanDefinitionHolders(providerScanner, scanBasePackage, registry, beanNameGenerator);
        if (CollectionUtils.isEmpty(providerBeanDefinitionHolders)) {
            return;
        }
        providerBeanDefinitionHolders.forEach(providerBeanDefinitionHolder -> {
            registerProviderWrapperBean(providerBeanDefinitionHolder, registry, providerScanner);
        });
    }

    private Set<BeanDefinitionHolder> findProviderBeanDefinitionHolders(ClassPathBeanDefinitionRegistryScanner providerScanner, String scanBasePackage,
                                                                        BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator) {
        // Find the components satisfying the condition
        Set<BeanDefinition> beanDefinitions = providerScanner.findCandidateComponents(scanBasePackage);
        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<>(beanDefinitions.size());
        beanDefinitions.forEach(beanDefinition -> {
            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            beanDefinitionHolders.add(new BeanDefinitionHolder(beanDefinition, beanName));
        });
        return beanDefinitionHolders;
    }

    private void registerProviderWrapperBean(BeanDefinitionHolder providerBeanDefinitionHolder, BeanDefinitionRegistry registry, ClassPathBeanDefinitionRegistryScanner providerScanner) {
        Class<?> providerBeanClass = resolveProviderClass(providerBeanDefinitionHolder);
        Provider providerAnnotation = findProviderAnnotation(providerBeanClass);
        Class<?> providerInterfaceClass = findProviderInterface(providerAnnotation, providerBeanClass);


        String providerWrapperBeanName = buildProviderWrapperBeanName(providerInterfaceClass);
        AbstractBeanDefinition wrapperBeanDefinition = buildProviderWrapperDefinition(ProviderWrapper.class, providerInterfaceClass, providerAnnotation, providerBeanDefinitionHolder.getBeanName());

        // Check duplicated candidate bean
        if (providerScanner.checkCandidate(providerWrapperBeanName, wrapperBeanDefinition)) {
            registry.registerBeanDefinition(providerWrapperBeanName, wrapperBeanDefinition);
            log.info("Registered RPC provider wrapper [{}] to spring context", providerWrapperBeanName);
        }
    }

    private Class<?> resolveProviderClass(BeanDefinitionHolder beanDefinitionHolder) {
        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        String beanClassName = beanDefinition.getBeanClassName();
        return ClassUtils.resolveClassName(beanClassName, classLoader);
    }

    private Provider findProviderAnnotation(Class<?> beanClass) {
        return beanClass.getAnnotation(Provider.class);
    }

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

    private String buildProviderWrapperBeanName(Class<?> interfaceClass) {
        return ProviderWrapperBeanNameBuilder.builder(interfaceClass, environment).build();
    }

    private AbstractBeanDefinition buildProviderWrapperDefinition(Class<?> providerWrapperClass, Class<?> providerInterfaceClass, Provider providerAnnotation, String providerInstanceName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(providerWrapperClass);
        addPropertyValue(builder, "interfaceName", providerInterfaceClass.getName(), providerWrapperClass, false);
        addPropertyValue(builder, "interfaceClass", providerInterfaceClass, providerWrapperClass, false);
        addPropertyValue(builder, "instanceName", providerInstanceName, providerWrapperClass, false);
        addPropertyValue(builder, "retries", providerAnnotation.maxRetries(), providerWrapperClass, true);
        addPropertyValue(builder, "checkHealth", providerAnnotation.checkHealth(), providerWrapperClass, false);
        // Obtain the instance by instance name and inject
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
     * Obtain the instance by instance name and inject
     *
     * @param builder      bean definition builder
     * @param propertyName property name
     * @param instanceName provider instance name
     */
    private void addPropertyReference(BeanDefinitionBuilder builder, String propertyName, String instanceName) {
        builder.addPropertyReference(propertyName, environment.resolvePlaceholders(instanceName));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory arg) throws BeansException {
        // Leave blank intentionally
    }
}