package org.infinity.luix.spring.boot.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.client.stub.ConsumerStub;
import org.infinity.luix.core.client.stub.ConsumerStubHolder;
import org.infinity.luix.core.common.RpcMethod;
import org.infinity.luix.core.config.impl.RegistryConfig;
import org.infinity.luix.core.registry.Registry;
import org.infinity.luix.core.registry.RegistryFactory;
import org.infinity.luix.core.server.buildin.BuildInService;
import org.infinity.luix.core.server.stub.MethodConfig;
import org.infinity.luix.core.server.stub.ProviderStub;
import org.infinity.luix.core.server.stub.ProviderStubHolder;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.spring.boot.config.LuixProperties;
import org.infinity.luix.utilities.destory.ShutdownHook;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.infinity.luix.core.constant.ProtocolConstants.PROTOCOL;
import static org.infinity.luix.core.constant.RegistryConstants.REGISTRY_VAL_NONE;
import static org.infinity.luix.core.constant.ServiceConstants.*;
import static org.infinity.luix.core.server.stub.ProviderStub.buildProviderStubBeanName;
import static org.infinity.luix.core.utils.MethodParameterUtils.getMethodSignature;
import static org.infinity.luix.spring.boot.utils.ProxyUtils.getTargetClass;

/**
 * Used to start and stop the RPC server
 */
@Slf4j
public class RpcLifecycle {
    private static final RpcLifecycle  INSTANCE = new RpcLifecycle();
    /**
     * Indicates whether the RPC server already started or not
     */
    private final        AtomicBoolean started  = new AtomicBoolean(false);
    /**
     * Indicates whether the RPC server already stopped or not
     */
    private final        AtomicBoolean stopped  = new AtomicBoolean(false);

    /**
     * Prevent instantiation of it outside the class
     */
    private RpcLifecycle() {
    }

    /**
     * Get the singleton instance
     *
     * @return singleton instance {@link RpcLifecycle}
     */
    public static RpcLifecycle getInstance() {
        return INSTANCE;
    }

    public AtomicBoolean getStarted() {
        return started;
    }

    public AtomicBoolean getStopped() {
        return stopped;
    }

    /**
     * Start the RPC server
     *
     * @param beanFactory    bean factory
     * @param luixProperties RPC configuration properties
     */
    public void start(DefaultListableBeanFactory beanFactory, LuixProperties luixProperties) {
        if (!started.compareAndSet(false, true)) {
            // already started
            return;
        }
        log.info("Starting the Luix RPC server");
        registerShutdownHook();
        registerBuildInProviderStubs(beanFactory, luixProperties);
        publish(luixProperties);
        subscribe(luixProperties);
        log.info("Started the Luix RPC server");
    }


    /**
     * Register the shutdown hook to system runtime
     */
    private void registerShutdownHook() {
        ShutdownHook.register();
    }

    /**
     * Register build-in provider stubs
     *
     * @param beanFactory    bean factory
     * @param luixProperties RPC configuration properties
     */
    private void registerBuildInProviderStubs(DefaultListableBeanFactory beanFactory,
                                              LuixProperties luixProperties) {
        String beanName = buildProviderStubBeanName(BuildInService.class.getName());
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ProviderStub.class);
        builder.addPropertyValue(BEAN_NAME, beanName);
        builder.addPropertyValue(INTERFACE_CLASS, BuildInService.class);
        builder.addPropertyValue(INTERFACE_NAME, BuildInService.class.getName());
        builder.addPropertyValue(PROTOCOL, luixProperties.getProtocol().getName());
        builder.addPropertyReference("instance", StringUtils.uncapitalize(BuildInService.class.getSimpleName()));

        beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
        // Method getBean() will trigger bean initialization
        beanFactory.getBean(beanName, ProviderStub.class);
    }

    /**
     * Publish RPC providers to registries
     *
     * @param luixProperties RPC configuration properties
     */
    private void publish(LuixProperties luixProperties) {
        luixProperties.getRegistryList().forEach(registryConfig -> {
            if (!registryConfig.getName().equals(REGISTRY_VAL_NONE)) {
                // Non-direct registry

                // Publish providers
                publishProviders(luixProperties, registryConfig);
            }
        });
    }

    private void publishProviders(LuixProperties luixProperties, RegistryConfig registryConfig) {
        Map<String, ProviderStub<?>> providerStubs = ProviderStubHolder.getInstance().getMap();
        if (MapUtils.isEmpty(providerStubs)) {
            log.info("No RPC service providers found to register to registry [{}]", registryConfig.getName());
            return;
        }
        providerStubs.forEach((name, providerStub) -> {
            // Set method level configuration
            Arrays.stream(getTargetClass(providerStub.getInstance()).getMethods()).forEach(method ->
                    setMethodConfig(providerStub, method)
            );
            if (providerStub.getInterfaceClass() != null) {
                Arrays.stream(providerStub.getInterfaceClass().getMethods()).forEach(method ->
                        setMethodConfig(providerStub, method)
                );
            }

            // Register providers
            providerStub.register(luixProperties.getApplication(), luixProperties.getAvailableProtocol(), registryConfig);
        });

        if (luixProperties.getProvider().isAutoExpose()) {
            // Activate RPC service providers
            providerStubs.forEach((name, providerStub) ->
                    providerStub.activate()
            );
        }
    }

    private void setMethodConfig(ProviderStub<?> providerStub, Method method) {
        RpcMethod annotation = AnnotationUtils.getAnnotation(method, RpcMethod.class);
        if (annotation != null) {
            MethodConfig methodConfig = MethodConfig.builder()
                    .retryCount(defaultIfEmpty(annotation.retryCount(), null))
                    .requestTimeout(defaultIfEmpty(annotation.requestTimeout(), null))
                    .build();
            providerStub.getMethodConfig().putIfAbsent(getMethodSignature(method), methodConfig);
        }
    }

    /**
     * Subscribe provider from registries
     *
     * @param luixProperties RPC configuration properties
     */
    private void subscribe(LuixProperties luixProperties) {
        Map<String, ConsumerStub<?>> consumerStubs = ConsumerStubHolder.getInstance().get();
        if (MapUtils.isEmpty(consumerStubs)) {
            log.info("No RPC consumers found on the registries");
            return;
        }
        consumerStubs.forEach((name, consumerStub) ->
                consumerStub.subscribeProviders(luixProperties.getApplication(),
                        luixProperties.getAvailableProtocol(),
                        luixProperties.getRegistryList()));
    }

    /**
     * Stop the RPC server
     *
     * @param luixProperties RPC configuration properties
     */
    public void destroy(LuixProperties luixProperties) {
        if (!started.compareAndSet(true, false) || !stopped.compareAndSet(false, true)) {
            // not yet started or already stopped
            return;
        }

        luixProperties.getRegistryList().forEach(registryConfig ->
                unregisterProviders(registryConfig.getRegistryUrl())
        );
    }

    /**
     * Unregister RPC providers from registry
     */
    private void unregisterProviders(Url... registryUrls) {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            if (registry == null || CollectionUtils.isEmpty(registry.getRegisteredProviderUrls())) {
                log.warn("No registry found!");
                return;
            }
            registry.getRegisteredProviderUrls().forEach(registry::unregister);
            log.debug("Unregistered all the RPC providers from registry [{}]", registryUrl.getProtocol());
        }
    }
}
