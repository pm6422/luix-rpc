package org.infinity.rpc.spring.boot.bean;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.server.stub.ProviderStub;
import org.infinity.rpc.core.server.annotation.Provider;
import org.infinity.rpc.spring.boot.bean.name.DefaultBeanNameGenerator;
import org.infinity.rpc.spring.boot.bean.registry.ClassPathBeanDefinitionRegistryScanner;
import org.infinity.rpc.spring.boot.bean.name.ProviderStubBeanNameBuilder;
import org.infinity.rpc.spring.boot.utils.AnnotationUtils;
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
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.infinity.rpc.core.constant.ServiceConstants.INTERFACE_CLASS;
import static org.infinity.rpc.core.constant.ServiceConstants.INTERFACE_NAME;
import static org.infinity.rpc.spring.boot.utils.AnnotationBeanDefinitionUtils.*;

/**
 * Register provider bean and provider stub under specified scan base packages to spring context
 * by {@link BeanDefinitionRegistry}
 */
@Slf4j
public class ProviderBeanDefinitionRegistryPostProcessor implements EnvironmentAware, ResourceLoaderAware,
        BeanClassLoaderAware, BeanDefinitionRegistryPostProcessor {

    private final Set<String>    scanBasePackages;
    private       Environment    env;
    private       ResourceLoader resourceLoader;
    private       ClassLoader    classLoader;

    public ProviderBeanDefinitionRegistryPostProcessor(Set<String> scanBasePackages) {
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.env = environment;
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
        Set<String> resolvedScanBasePackages = resolvePackagePlaceholders();
        if (CollectionUtils.isEmpty(resolvedScanBasePackages)) {
            log.warn("No package to be scanned for registering providers!");
            return;
        }
        doRegisterProviderBeans(registry, resolvedScanBasePackages);
    }

    /**
     * Resolve the placeholder in package name
     *
     * @return replaced packages
     */
    private Set<String> resolvePackagePlaceholders() {
        return scanBasePackages
                .stream()
                .filter(StringUtils::hasText)
                .map(x -> env.resolvePlaceholders(x.trim()))
                .collect(Collectors.toSet());
    }

    /**
     * Register provider and provider stub beans
     *
     * @param registry                 current bean definition registry
     * @param resolvedScanBasePackages provider packages to be scanned
     */
    private void doRegisterProviderBeans(BeanDefinitionRegistry registry, Set<String> resolvedScanBasePackages) {
        BeanNameGenerator beanNameGenerator = DefaultBeanNameGenerator.create();
        ClassPathBeanDefinitionRegistryScanner providerScanner = createProviderScanner(registry, beanNameGenerator);

        resolvedScanBasePackages.forEach(scanBasePackage -> {
            // Register provider stub first
            boolean registered = registerProviderStubBeans(registry, beanNameGenerator, providerScanner, scanBasePackage);
            if (registered) {
                // Then register provider beans beans
                registerProviderBeans(providerScanner, scanBasePackage);
                log.info("Registered RPC provider instances to spring context");
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
     * '@Provider(maxRetries=1)
     * public class AppServiceImpl {
     * ...
     * ...
     * }'
     * </code>
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
    }

    /**
     * Register provider stub {@link ProviderStub} beans
     *
     * @param registry          current bean definition registry
     * @param beanNameGenerator bean name generator
     * @param providerScanner   provider bean definition registry scanner
     * @param scanBasePackage   provider packages to be scanned
     * @return true: registered provider stub, false: no provider stub registered
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
        Provider providerAnnotation = findProviderAnnotation(providerInstanceClass);
        Class<?> providerInterfaceClass = resolveProviderInterface(providerAnnotation, providerInstanceClass);

        String providerStubBeanName = buildProviderStubBeanName(providerInterfaceClass,
                providerAnnotation.group(), providerAnnotation.version());
        AbstractBeanDefinition stubBeanDefinition = buildProviderStubDefinition(
                providerInterfaceClass, providerAnnotation, providerBeanDefinitionHolder.getBeanName());

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
     * Build provider stub bean name
     *
     * @param interfaceClass provider interface class
     * @param group          group
     * @param version        version
     * @return provider stub bean name
     */
    private String buildProviderStubBeanName(Class<?> interfaceClass, String group, String version) {
        return ProviderStubBeanNameBuilder
                .builder(interfaceClass.getName(), env)
                .group(group)
                .version(version)
                .build();
    }

    /**
     * Build {@link ProviderStub} definition
     *
     * @param interfaceClass       provider interface class
     * @param annotation           {@link Provider} annotation
     * @param providerInstanceName provider instance name
     * @return {@link ProviderStub} bean definition
     */
    private AbstractBeanDefinition buildProviderStubDefinition(Class<?> interfaceClass,
                                                               Provider annotation,
                                                               String providerInstanceName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ProviderStub.class);
        copyPropertiesToBeanDefinitionBuilder(annotation, builder, INTERFACE_NAME);
        addPropertyValue(builder, INTERFACE_NAME, interfaceClass.getName());
        addPropertyValue(builder, INTERFACE_CLASS, interfaceClass);
        // Obtain the instance by instance name then assign it to the property
        addPropertyReference(builder, "instance", providerInstanceName, env);
        return builder.getBeanDefinition();
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory arg) throws BeansException {
        // Leave blank intentionally
    }
}