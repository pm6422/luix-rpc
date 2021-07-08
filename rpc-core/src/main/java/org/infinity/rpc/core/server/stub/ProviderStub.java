package org.infinity.rpc.core.server.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.config.impl.ProtocolConfig;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.impl.RpcBizException;
import org.infinity.rpc.core.exception.impl.RpcConfigException;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;
import org.infinity.rpc.core.utils.name.ProviderStubBeanNameBuilder;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.infinity.rpc.core.constant.ApplicationConstants.APP;
import static org.infinity.rpc.core.constant.ProtocolConstants.*;
import static org.infinity.rpc.core.constant.ProviderConstants.HEALTH_CHECKER;
import static org.infinity.rpc.core.constant.ServiceConstants.*;
import static org.infinity.rpc.core.server.response.impl.RpcCheckHealthResponse.STATUS_INACTIVE;
import static org.infinity.rpc.core.server.response.impl.RpcCheckHealthResponse.STATUS_OK;
import static org.infinity.rpc.core.url.Url.METHOD_CONFIG_PREFIX;
import static org.infinity.rpc.core.utils.MethodParameterUtils.getMethodSignature;

/**
 * RPC provider stub
 * A stub in distributed computing is a piece of code that converts parameters passed between client and server
 * during a remote procedure call(RPC).
 * A provider stub take charge of handling remote method invocation and delegate to associate local method to execute.
 *
 * @param <T>: The interface class of the provider
 */
@Slf4j
@Setter
@Getter
public class ProviderStub<T> {
    /**
     * Provider stub bean name
     */
    @NotNull(message = "The [beanName] property must NOT be null!")
    private           String                    beanName;
    /**
     * The interface class of the provider
     */
    @NotNull(message = "The [interfaceClass] property of @Provider must NOT be null!")
    private           Class<T>                  interfaceClass;
    /**
     * The provider interface fully-qualified name
     */
    @NotEmpty(message = "The [interfaceName] property of @Provider must NOT be empty!")
    private           String                    interfaceName;
    /**
     * Protocol
     */
    @NotEmpty(message = "The [protocol] property of @Provider must NOT be empty!")
    private           String                    protocol;
    /**
     * Serializer used to serialize and deserialize object
     */
    private           String                    serializer;
    /**
     * One service interface may have multiple implementations(forms),
     * It used to distinguish between different implementations of service provider interface
     */
    private           String                    form;
    /**
     * When the service changes, such as adding or deleting methods, and interface parameters change,
     * the provider and consumer application instances need to be upgraded.
     * In order to deploy in a production environment without affecting user use,
     * a gradual migration scheme is generally adopted.
     * First upgrade some provider application instances,
     * and then use the same version number to upgrade some consumer instances.
     * The old version of the consumer instance calls the old version of the provider instance.
     * Observe that there is no problem and repeat this process to complete the upgrade.
     */
    private           String                    version;
    /**
     *
     */
    private           String                    healthChecker;
    /**
     *
     */
    @Min(value = 0, message = "The [timeout] property of @Provider must NOT be a negative number!")
    private           Integer                   requestTimeout;
    /**
     * The max retry count of RPC request
     */
    @Min(value = 0, message = "The [retryCount] property of @Provider must NOT be a negative number!")
    @Max(value = 10, message = "The [retryCount] property of @Provider must NOT be bigger than 10!")
    private           Integer                   retryCount;
    /**
     * The max response message payload size in bytes
     */
    @Min(value = 0, message = "The [maxPayload] property of @Provider must NOT be a positive number!")
    private           Integer                   maxPayload;
    /**
     * The provider instance
     * Disable deserialization
     */
    @NotNull
    private transient T                         instance;
    /**
     * Method signature to method cache map for the provider class
     */
    private transient Map<String, Method>       methodsCache    = new HashMap<>();
    /**
     * Method signature to method level configuration map for the provider class
     */
    private           Map<String, MethodConfig> methodConfig    = new HashMap<>();
    /**
     * All the methods of the interface class
     */
    private           List<MethodData>          methodDataCache = new ArrayList<>();
    /**
     * The provider url
     */
    private           Url                       url;
    /**
     * Service provider exporter used to export the provider to registry
     */
    private           Exportable<T>             exporter;
    /**
     * Indicator used to identify whether the provider already been registered
     */
    private final     AtomicBoolean             exported        = new AtomicBoolean(false);
    /**
     * Protocol configuration
     */
    private           ProtocolConfig            protocolConfig;
    /**
     * Registry configuration
     */
    private           RegistryConfig            registryConfig;

    /**
     * The method is invoked by Java EE container automatically after registered bean definition
     */
    @PostConstruct
    public void init() {
        // Put methods to cache in order to accelerate the speed of executing.
        discoverMethods(interfaceClass);
        if (StringUtils.isNotEmpty(beanName)) {
            // Automatically add {@link ProviderStub} instance to {@link ProviderStubHolder}
            ProviderStubHolder.getInstance().add(beanName, this);
        }
    }

    /**
     * Discover all methods of provider interface and put them all to cache
     *
     * @param interfaceClass The interface class of the provider
     */
    private void discoverMethods(Class<T> interfaceClass) {
        // Get all methods of the class passed in or its super interfaces.
        Arrays.stream(interfaceClass.getMethods()).forEach(method -> {
            String methodSignature = getMethodSignature(method);
            methodsCache.putIfAbsent(methodSignature, method);
            List<String> methodParameters = Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.toList());
            MethodData methodData = new MethodData(method.getName(), methodParameters, methodSignature, method.getGenericReturnType().getTypeName());
            methodDataCache.add(methodData);
        });
    }

    /**
     * Find method associated with name and parameter list
     *
     * @param methodName       method name
     * @param methodParameters method parameters string. e.g, java.util.List,java.lang.Long
     * @return method
     */
    public Method findMethod(String methodName, String methodParameters) {
        return methodsCache.get(getMethodSignature(methodName, methodParameters));
    }

    /**
     * Register the RPC providers to registries
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registryConfig    registry configuration
     */
    public void register(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig, RegistryConfig registryConfig) {
        if (exported.compareAndSet(false, true)) {
            this.protocolConfig = protocolConfig;
            this.registryConfig = registryConfig;

            // Export provider url
            url = createProviderUrl(applicationConfig, protocolConfig);
            // Export RPC provider service
            exporter = Protocol.getInstance(url.getProtocol()).export(this);
            // Register provider URL to all the registries
            this.registryConfig.getRegistryImpl().register(url);
        }
    }

    /**
     * Unregister current RPC provider from registry
     */
    public void unregister(Url... registryUrls) {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            if (registry == null || CollectionUtils.isEmpty(registry.getRegisteredProviderUrls())) {
                log.warn("No registry found!");
                return;
            }
            registry.unregister(url);
            log.debug("Unregistered RPC provider [{}] from registry [{}]", interfaceName, registryUrl.getProtocol());
        }
    }

    /**
     * Deactivate the RPC providers from registries
     */
    public void deactivate() {
        if (exported.get() && exporter != null) {
            this.registryConfig.getRegistryImpl().deactivate(url);
            exporter.cancelExport();
            exported.set(false);
        }
    }

    /**
     * Activate the RPC providers from registries
     */
    public void activate() {
        if (!exported.get() && exporter != null) {
            this.registryConfig.getRegistryImpl().activate(url);
            Protocol.getInstance(url.getProtocol()).export(this);
            exported.set(true);
        }
    }

    /**
     * Merge high priority properties to provider stub and generate provider url
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @return provider url
     */
    private Url createProviderUrl(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig) {
        Url url = Url.providerUrl(protocol, protocolConfig.getHost(), protocolConfig.getPort(), interfaceName, form, version);
        url.addOption(SERIALIZER, serializer);
        url.addOption(APP, applicationConfig.getName());
        url.addOption(HEALTH_CHECKER, healthChecker);
        url.addOption(REQUEST_TIMEOUT, requestTimeout);
        url.addOption(RETRY_COUNT, retryCount);
        url.addOption(MAX_PAYLOAD, maxPayload);

        url.addOption(CODEC, protocolConfig.getCodec());
        url.addOption(ENDPOINT_FACTORY, protocolConfig.getEndpointFactory());

        String minClientConn = protocolConfig.getMinClientConn() == null ? null : protocolConfig.getMinClientConn().toString();
        url.addOption(MIN_CLIENT_CONN, minClientConn);

        String maxClientFailedConn = protocolConfig.getMaxClientFailedConn() == null ? null : protocolConfig.getMaxClientFailedConn().toString();
        url.addOption(MAX_CLIENT_FAILED_CONN, maxClientFailedConn);

        String maxServerConn = protocolConfig.getMaxServerConn() == null ? null : protocolConfig.getMaxServerConn().toString();
        url.addOption(MAX_SERVER_CONN, maxServerConn);

        String maxContentLength = protocolConfig.getMaxContentLength() == null ? null : protocolConfig.getMaxContentLength().toString();
        url.addOption(MAX_CONTENT_LENGTH, maxContentLength);

        String minThread = protocolConfig.getMinThread() == null ? null : protocolConfig.getMinThread().toString();
        url.addOption(MIN_THREAD, minThread);

        String maxThread = protocolConfig.getMaxThread() == null ? null : protocolConfig.getMaxThread().toString();
        url.addOption(MAX_THREAD, maxThread);

        String workQueueSize = protocolConfig.getWorkQueueSize() == null ? null : protocolConfig.getWorkQueueSize().toString();
        url.addOption(WORK_QUEUE_SIZE, workQueueSize);

        String sharedChannel = protocolConfig.getSharedChannel() == null ? null : protocolConfig.getSharedChannel().toString();
        url.addOption(SHARED_CHANNEL, sharedChannel);

        String asyncInitConn = protocolConfig.getAsyncInitConn() == null ? null : protocolConfig.getAsyncInitConn().toString();
        url.addOption(ASYNC_INIT_CONN, asyncInitConn);

        if (MapUtils.isNotEmpty(methodConfig)) {
            for (Map.Entry<String, MethodConfig> entry : methodConfig.entrySet()) {
                for (Field field : MethodConfig.class.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        if (!Modifier.isStatic(field.getModifiers()) && field.get(entry.getValue()) != null) {
                            String name = METHOD_CONFIG_PREFIX + entry.getKey() + "." + field.getName();
                            url.addOption(name, field.get(entry.getValue()).toString());
                        }
                    } catch (IllegalAccessException e) {
                        throw new RpcConfigException("Failed to read method configuration", e);
                    }
                }
            }
        }
        return url;
    }

    /**
     * Invoke method locally and return the result
     *
     * @param request RPC request
     * @return RPC response
     */
    public Responseable localInvoke(Requestable request) {
        RpcFrameworkUtils.logEvent(request, RpcConstants.TRACE_BEFORE_BIZ);
        Responseable response = doLocalInvoke(request);
        RpcFrameworkUtils.logEvent(response, RpcConstants.TRACE_AFTER_BIZ);
        return response;
    }

    /**
     * Do the invocation of method
     *
     * @param request RPC request
     * @return RPC response
     */
    private Responseable doLocalInvoke(Requestable request) {
        RpcResponse response = new RpcResponse();
        Method method = findMethod(request.getMethodName(), request.getMethodParameters());
        if (method == null) {
            String methodSignature = getMethodSignature(request.getMethodName(), request.getMethodParameters());
            RpcFrameworkException exception =
                    new RpcFrameworkException("Method [" + methodSignature + "] of service [" + request.getInterfaceName() + "] not found!");
            response.setException(exception);
            return response;
        }
        boolean defaultThrowExceptionStack = TRANS_EXCEPTION_STACK_VAL_DEFAULT;
        try {
            // Invoke method
            Object result = method.invoke(instance, request.getMethodArguments());
            response.setResult(result);
        } catch (Exception e) {
            if (e.getCause() != null) {
                response.setException(new RpcBizException("Failed to call provider stub [" + getMethodSignature(method) + "]", e.getCause()));
            } else {
                response.setException(new RpcBizException("Failed to call provider stub [" + getMethodSignature(method) + "]", e));
            }

            // not print stack in error log when exception declared in method
            boolean logException = true;
            for (Class<?> clazz : method.getExceptionTypes()) {
                if (clazz.isInstance(response.getException().getCause())) {
                    logException = false;
                    defaultThrowExceptionStack = false;
                    break;
                }
            }
            if (logException) {
                log.error("Exception caught when during method invocation. request:" + request, e);
            } else {
                log.info("Exception caught when during method invocation. request:" + request + ", exception:" + response.getException().getCause().toString());
            }
        } catch (Throwable t) {
            // 如果服务发生Error，将Error转化为Exception，防止拖垮调用方
            if (t.getCause() != null) {
                response.setException(new RpcFrameworkException("Failed to invoke service provider", t.getCause()));
            } else {
                response.setException(new RpcFrameworkException("Failed to invoke service provider", t));
            }
            //对于Throwable,也记录日志
            log.error("Exception caught when during method invocation. request:" + request, t);
        }
        if (response.getException() != null) {
            //是否传输业务异常栈
            if (!this.url.getBooleanOption(TRANS_EXCEPTION_STACK, defaultThrowExceptionStack)) {
                // 不传输业务异常栈
                ExceptionUtils.setMockStackTrace(response.getException().getCause());
            }
        }
        // Copy options
        response.setOptions(request.getOptions());
        return response;
    }

    /**
     * Build provider stub bean name
     *
     * @param interfaceClassName provider interface class name
     * @return provider stub bean name
     */
    public static String buildProviderStubBeanName(String interfaceClassName) {
        return buildProviderStubBeanName(interfaceClassName, null, null);
    }

    /**
     * Build provider stub bean name
     *
     * @param interfaceClassName provider interface class name
     * @param form               form
     * @param version            version
     * @return provider stub bean name
     */
    public static String buildProviderStubBeanName(String interfaceClassName, String form, String version) {
        return ProviderStubBeanNameBuilder
                .builder(interfaceClassName)
                .form(form)
                .version(version)
                .build();
    }

    /**
     * Check health status of the service
     *
     * @return status
     */
    public String checkHealth() {
        return exported.get() ? STATUS_OK : STATUS_INACTIVE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, interfaceName, form, version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProviderStub<?> providerStub = (ProviderStub<?>) o;
        return Objects.equals(protocol, providerStub.protocol)
                && Objects.equals(interfaceName, providerStub.interfaceName)
                && Objects.equals(form, providerStub.form)
                && Objects.equals(version, providerStub.version);
    }
}
