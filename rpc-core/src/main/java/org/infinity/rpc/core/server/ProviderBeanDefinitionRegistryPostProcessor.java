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

import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;

@Slf4j
public class ProviderBeanDefinitionRegistryPostProcessor implements EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware, BeanDefinitionRegistryPostProcessor {

    private Set<String>         scanBasePackages;
    private Environment         environment;
    private ResourceLoader      resourceLoader;
    private ClassLoader         classLoader;

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
        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(registry);
        RpcClassPathBeanDefinitionScanner scanner = createClassPathScanner(registry, beanNameGenerator);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Provider.class));

        for (String scanBasePackage : scanBasePackages) {
            // Register provider bean
            // Note: scanner.scan can lead to register @Provider Bean
            scanner.scan(scanBasePackage);
            log.info("Registered all RPC service providers");
//            Set<BeanDefinitionHolder> foundResults = findServiceBeanDefinitionHolders(scanner, scanBasePackage, registry, beanNameGenerator);
//            if (!CollectionUtils.isEmpty(foundResults)) {
//                for (BeanDefinitionHolder foundResult : foundResults) {
//                    registerBean(foundResult, registry, scanner);
//                    log.info("Registered RPC service provider [{}]", foundResult.getBeanName());
//                }
//            }
        }
    }

    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry registry) {
        BeanNameGenerator beanNameGenerator = null;
        if (registry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry singletonBeanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGenerator = (BeanNameGenerator) singletonBeanRegistry.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
        }

        if (beanNameGenerator == null) {
            if (log.isInfoEnabled()) {
                log.info("BeanNameGenerator bean can't be found in BeanFactory with name [" + CONFIGURATION_BEAN_NAME_GENERATOR + "]");
                log.info("BeanNameGenerator will be a instance of " + AnnotationBeanNameGenerator.class.getName() + " , it maybe a potential problem on bean name generation.");
            }
            beanNameGenerator = new AnnotationBeanNameGenerator();
        }
        return beanNameGenerator;
    }

    private RpcClassPathBeanDefinitionScanner createClassPathScanner(BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator) {
        RpcClassPathBeanDefinitionScanner scanner = new RpcClassPathBeanDefinitionScanner(registry, environment, resourceLoader);
        scanner.setBeanNameGenerator(beanNameGenerator);
        return scanner;
    }

    private Set<BeanDefinitionHolder> findServiceBeanDefinitionHolders(RpcClassPathBeanDefinitionScanner scanner, String scanBasePackage, BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator) {
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

    private void registerBean(BeanDefinitionHolder beanDefinitionHolder, BeanDefinitionRegistry registry, RpcClassPathBeanDefinitionScanner scanner) {
        Class<?> providerBeanClass = resolveClass(beanDefinitionHolder);
        Provider providerAnnotation = providerBeanClass.getAnnotation(Provider.class);
//        AnnotationAttributes annotationAttributes = AnnotationUtils.getAnnotationAttributes(providerAnnotation, false, false);
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

        BeanDefinitionBuilder builder = rootBeanDefinition(interfaceClass);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();

//        String serviceBeanName = beanDefinitionHolder.getBeanName();
//        if (scanner.checkCandidate(serviceBeanName, beanDefinition)) { // Check duplicated candidate bean
//            registry.registerBeanDefinition(serviceBeanName, beanDefinition);
//        }
    }

    private Class<?> resolveClass(BeanDefinitionHolder beanDefinitionHolder) {
        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        String beanClassName = beanDefinition.getBeanClassName();
        return ClassUtils.resolveClassName(beanClassName, classLoader);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory arg) throws BeansException {
        // Leave blank intentionally
    }
}