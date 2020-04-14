package org.infinity.rpc.core.server;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.registrar.RpcClassPathBeanDefinitionScanner;
import org.infinity.rpc.core.server.annotation.Provider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;

@Slf4j
public class ProviderBeanDefinitionRegistryPostProcessor implements EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware, BeanDefinitionRegistryPostProcessor {

    private Set<String>    scanBasePackages;
    private Environment    environment;
    private ResourceLoader resourceLoader;
    private ClassLoader    classLoader;

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
//        registerListener(registry, );
        registerBeans(registry, scanBasePackages);
    }

    private void registerBeans(BeanDefinitionRegistry registry, Set<String> scanBasePackages) {
        Set<String> resolvedScanBasePackages = resolvePackagePlaceHolders(scanBasePackages);
        if (CollectionUtils.isEmpty(resolvedScanBasePackages)) {
            log.warn("No scan package to register bean!");
            return;
        }
        registerProviderBeans(registry, resolvedScanBasePackages);
    }

    private Set<String> resolvePackagePlaceHolders(Set<String> scanBasePackages) {
        Set<String> resolvedPkgs = scanBasePackages.stream()
                .filter(x -> StringUtils.hasText(x))
                .map(x -> environment.resolvePlaceholders(x.trim()))
                .collect(Collectors.toSet());
        return resolvedPkgs;
    }

    private void registerProviderBeans(BeanDefinitionRegistry registry, Set<String> resolvedScanBasePackages) {
        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(registry);
        RpcClassPathBeanDefinitionScanner scanner = createClassPathScanner(registry, beanNameGenerator);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Provider.class));

        for (String scanBasePackage : resolvedScanBasePackages) {
            // The 'scan' method can register @Provider Bean to spring context
            scanner.scan(scanBasePackage);

            // Next we need to register ProviderBean which is the wrapper of service provider to spring context
            Set<BeanDefinitionHolder> providerBeanDefinitionHolders = findProviderBeanDefinitionHolders(scanner, scanBasePackage, registry, beanNameGenerator);
            if (!CollectionUtils.isEmpty(providerBeanDefinitionHolders)) {
                for (BeanDefinitionHolder providerBeanDefinitionHolder : providerBeanDefinitionHolders) {
                    registerProviderBean(providerBeanDefinitionHolder, registry, scanner);
                }
            }
            log.info("Registered all RPC service providers");
        }
    }

    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry registry) {
        BeanNameGenerator beanNameGenerator = null;
        if (registry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry singletonBeanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGenerator = (BeanNameGenerator) singletonBeanRegistry.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
        }
        if (beanNameGenerator == null) {
            log.info("Can NOT find BeanNameGenerator bean with name [{}]", CONFIGURATION_BEAN_NAME_GENERATOR);
            log.info("Using the default [{}]", AnnotationBeanNameGenerator.class.getName());
            beanNameGenerator = new AnnotationBeanNameGenerator();
        }
        return beanNameGenerator;
    }

    private RpcClassPathBeanDefinitionScanner createClassPathScanner(BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator) {
        RpcClassPathBeanDefinitionScanner scanner = new RpcClassPathBeanDefinitionScanner(registry, environment, resourceLoader);
        scanner.setBeanNameGenerator(beanNameGenerator);
        return scanner;
    }

    private Set<BeanDefinitionHolder> findProviderBeanDefinitionHolders(RpcClassPathBeanDefinitionScanner scanner, String scanBasePackage, BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator) {
        // Find the components satisfying the condition
        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(scanBasePackage);
        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<>(beanDefinitions.size());
        for (BeanDefinition beanDefinition : beanDefinitions) {
            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
            beanDefinitionHolders.add(beanDefinitionHolder);
        }

        return beanDefinitionHolders;
    }

    private void registerProviderBean(BeanDefinitionHolder providerBeanDefinitionHolder, BeanDefinitionRegistry registry, RpcClassPathBeanDefinitionScanner scanner) {
        Class<?> providerBeanClass = resolveClass(providerBeanDefinitionHolder);
        Provider providerAnnotation = findProviderAnnotation(providerBeanClass);
        Class<?>[] interfaceClasses = providerBeanClass.getInterfaces();
        Class<?> interfaceClass;

        if (interfaceClasses.length == 0) {
            throw new IllegalStateException("The RPC service provider bean must implement more than one interfaces!");
        } else if (interfaceClasses.length == 1) {
            interfaceClass = interfaceClasses[0];
        } else {
            // Get service interface from annotation if a instance has more than one declared interfaces
            interfaceClass = providerAnnotation.interfaceClass();
            if (void.class.equals(providerAnnotation.interfaceClass())) {
                throw new IllegalStateException("The @Provider annotation of RPC service provider must specify interfaceClass attribute value " +
                        "if the bean implements more than one interfaces!");
            }
        }

        String providerBeanName = generateProviderBeanName(interfaceClass);
        AbstractBeanDefinition beanDefinition = buildProviderBeanDefinition(ProviderBean.class, interfaceClass, providerBeanDefinitionHolder.getBeanName());

        // Check duplicated candidate bean
        if (scanner.checkCandidate(providerBeanName, beanDefinition)) {
            // Register bean definition
            registry.registerBeanDefinition(providerBeanName, beanDefinition);
            log.info("Registered RPC provider [{}]", providerBeanName);
        }
    }

    private Provider findProviderAnnotation(Class<?> beanClass) {
        return beanClass.getAnnotation(Provider.class);
    }

    private AbstractBeanDefinition buildProviderBeanDefinition(Class<?> providerBeanClass, Class<?> providerBeanInterfaceClass, String annotatedProviderBeanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(providerBeanClass);
        // References "ref" property to annotated @Provider Bean
//        addPropertyReference(builder, "ref", annotatedProviderBeanName);
        // Set interface
//        builder.addPropertyValue("interface", interfaceClass.getName());
        return builder.getBeanDefinition();
    }

    private void addPropertyReference(BeanDefinitionBuilder builder, String propertyName, String beanName) {
        String resolvedBeanName = environment.resolvePlaceholders(beanName);
        builder.addPropertyReference(propertyName, resolvedBeanName);
    }

    private Class<?> resolveClass(BeanDefinitionHolder beanDefinitionHolder) {
        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        String beanClassName = beanDefinition.getBeanClassName();
        return ClassUtils.resolveClassName(beanClassName, classLoader);
    }

    private String generateProviderBeanName(Class<?> interfaceClass) {
        ProviderBeanNameBuilder builder = ProviderBeanNameBuilder.create(interfaceClass, environment);
        return builder.build();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory arg) throws BeansException {
        // Leave blank intentionally
    }
}