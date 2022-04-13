package com.luixtech.rpc.core.server.stub;

import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.config.impl.ApplicationConfig;
import com.luixtech.rpc.core.config.impl.ProtocolConfig;
import com.luixtech.rpc.core.config.impl.RegistryConfig;
import com.luixtech.rpc.core.constant.ApplicationConstants;
import com.luixtech.rpc.core.constant.ProtocolConstants;
import com.luixtech.rpc.core.constant.ProviderConstants;
import com.luixtech.rpc.core.constant.ServiceConstants;
import com.luixtech.rpc.core.exception.ExceptionUtils;
import com.luixtech.rpc.core.exception.impl.RpcBizException;
import com.luixtech.rpc.core.exception.impl.RpcConfigException;
import com.luixtech.rpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.rpc.core.protocol.Protocol;
import com.luixtech.rpc.core.registry.Registry;
import com.luixtech.rpc.core.registry.factory.RegistryFactory;
import com.luixtech.rpc.core.server.response.impl.RpcCheckHealthResponse;
import com.luixtech.rpc.core.server.response.impl.RpcResponse;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.core.utils.MethodParameterUtils;
import com.luixtech.rpc.core.utils.name.ProviderStubBeanNameBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import com.luixtech.rpc.core.server.response.Responseable;

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

    public static final            String              METHOD_CHECK_HEALTH     = "@checkHealth";
    public static final            String              METHOD_GET_METHOD_METAS = "@getMethodMetas";
    public static final            String              METHOD_ACTIVATE         = "@activate";
    public static final            String              METHOD_DEACTIVATE       = "@deactivate";
    public static final            String              METHOD_REREGISTER       = "@reregister";
    public static final            List<String>        BUILD_IN_METHODS        = Arrays.asList(METHOD_CHECK_HEALTH,
            METHOD_GET_METHOD_METAS, METHOD_ACTIVATE, METHOD_DEACTIVATE, METHOD_REREGISTER);
    public static final            List<OptionMeta>    OPTIONS                 = new ArrayList<>();
    /**
     * Build-in method signature to method cache map
     */
    private static final transient Map<String, Method> BUILD_IN_METHODS_CACHE  = new HashMap<>();

    static {
//        OPTIONS.add(new OptionMeta(FORM, null, String.class.getSimpleName()));
//        OPTIONS.add(new OptionMeta(VERSION, null, String.class.getSimpleName()));
//        OPTIONS.add(new OptionMeta(APP, null, String.class.getSimpleName()));
        OPTIONS.add(new OptionMeta(ProtocolConstants.SERIALIZER, ProtocolConstants.SERIALIZERS, List.class.getSimpleName(), ProtocolConstants.SERIALIZER_VAL_DEFAULT, false));
//        OPTIONS.add(new OptionMeta(ProviderConstants.HEALTH_CHECKER, null, String.class.getSimpleName(), ProviderConstants.HEALTH_CHECKER_VAL_V1, true));
        OPTIONS.add(new OptionMeta(ServiceConstants.REQUEST_TIMEOUT, null, Integer.class.getSimpleName(), String.valueOf(ServiceConstants.REQUEST_TIMEOUT_VAL_DEFAULT), true));
        OPTIONS.add(new OptionMeta(ServiceConstants.RETRY_COUNT, null, Integer.class.getSimpleName(), String.valueOf(ServiceConstants.RETRY_COUNT_VAL_DEFAULT), true));
        OPTIONS.add(new OptionMeta(ServiceConstants.MAX_PAYLOAD, null, Integer.class.getSimpleName(), String.valueOf(ServiceConstants.MAX_PAYLOAD_VAL_DEFAULT), true));
//        OPTIONS.add(new OptionMeta(ProtocolConstants.CODEC, null, String.class.getSimpleName(), ProtocolConstants.CODEC_VAL_DEFAULT, true));
//        OPTIONS.add(new OptionMeta(ProtocolConstants.NETWORK_TRANSMISSION, null, String.class.getSimpleName(), ProtocolConstants.NETWORK_TRANSMISSION_VAL_NETTY, true));
//        OPTIONS.add(new OptionMeta(ProtocolConstants.SHARED_SERVER, null, Boolean.class.getSimpleName(), String.valueOf(ProtocolConstants.SHARED_SERVER_VAL_DEFAULT), false));
        OPTIONS.add(new OptionMeta(ProtocolConstants.MIN_CLIENT_CONN, null, Integer.class.getSimpleName(), String.valueOf(ProtocolConstants.MIN_CLIENT_CONN_VAL_DEFAULT), true));
        OPTIONS.add(new OptionMeta(ProtocolConstants.MAX_CLIENT_FAILED_CONN, null, Integer.class.getSimpleName(), String.valueOf(ProtocolConstants.MAX_CLIENT_FAILED_CONN_VAL_DEFAULT), true));
        OPTIONS.add(new OptionMeta(ProtocolConstants.MAX_SERVER_CONN, null, Integer.class.getSimpleName(), String.valueOf(ProtocolConstants.MAX_SERVER_CONN_VAL_DEFAULT), true));
        OPTIONS.add(new OptionMeta(ProtocolConstants.MAX_CONTENT_LENGTH, null, Integer.class.getSimpleName(), String.valueOf(ProtocolConstants.MAX_CONTENT_LENGTH_VAL_DEFAULT), true));
        OPTIONS.add(new OptionMeta(ProtocolConstants.MIN_THREAD, null, Integer.class.getSimpleName(), String.valueOf(ProtocolConstants.MIN_THREAD_VAL_DEFAULT), true));
        OPTIONS.add(new OptionMeta(ProtocolConstants.MAX_THREAD, null, Integer.class.getSimpleName(), String.valueOf(ProtocolConstants.MAX_THREAD_VAL_DEFAULT), true));
        OPTIONS.add(new OptionMeta(ProtocolConstants.WORK_QUEUE_SIZE, null, Integer.class.getSimpleName(), String.valueOf(ProtocolConstants.WORK_QUEUE_SIZE_VAL_DEFAULT), true));
//        OPTIONS.add(new OptionMeta(ProtocolConstants.ASYNC_CREATE_CONN, null, Boolean.class.getSimpleName(), String.valueOf(ProtocolConstants.ASYNC_CREATE_CONN_VAL_DEFAULT), true));

        try {
            // Add build-in methods
            Method checkHealthMethod = ProviderStub.class.getMethod(METHOD_CHECK_HEALTH.substring(1));
            BUILD_IN_METHODS_CACHE.putIfAbsent(MethodParameterUtils.getMethodSignature(METHOD_CHECK_HEALTH, MethodParameterUtils.getMethodParameters(checkHealthMethod)), checkHealthMethod);

            Method getMethodMetasMethod = ProviderStub.class.getMethod(METHOD_GET_METHOD_METAS.substring(1));
            BUILD_IN_METHODS_CACHE.putIfAbsent(MethodParameterUtils.getMethodSignature(METHOD_GET_METHOD_METAS, MethodParameterUtils.getMethodParameters(getMethodMetasMethod)), getMethodMetasMethod);

            Method activateMethod = ProviderStub.class.getMethod(METHOD_ACTIVATE.substring(1));
            BUILD_IN_METHODS_CACHE.putIfAbsent(MethodParameterUtils.getMethodSignature(METHOD_ACTIVATE, MethodParameterUtils.getMethodParameters(activateMethod)), activateMethod);

            Method deactivateMethod = ProviderStub.class.getMethod(METHOD_DEACTIVATE.substring(1));
            BUILD_IN_METHODS_CACHE.putIfAbsent(MethodParameterUtils.getMethodSignature(METHOD_DEACTIVATE, MethodParameterUtils.getMethodParameters(deactivateMethod)), deactivateMethod);

            Method reregisterMethod = ProviderStub.class.getMethod(METHOD_REREGISTER.substring(1), Map.class);
            BUILD_IN_METHODS_CACHE.putIfAbsent(MethodParameterUtils.getMethodSignature(METHOD_REREGISTER, MethodParameterUtils.getMethodParameters(reregisterMethod)), reregisterMethod);
        } catch (NoSuchMethodException e) {
            log.error("Failed to discover method!", e);
        }
    }

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
    private transient Map<String, Method>       methodsCache = new HashMap<>();
    /**
     * Method signature to method level configuration map for the provider class
     */
    private           Map<String, MethodConfig> methodConfig = new HashMap<>();
    /**
     * All the methods of the interface class
     */
    private       List<MethodMeta>  methodMetas  = new ArrayList<>();
    /**
     * The provider url
     */
    private       Url               url;
    /**
     * Indicates whether the provider were active
     */
    private final AtomicBoolean     activated    = new AtomicBoolean(false);
    /**
     * Application configuration
     */
    private       ApplicationConfig applicationConfig;
    /**
     * Protocol configuration
     */
    private       ProtocolConfig    protocolConfig;
    /**
     * Registry configuration
     */
    private       RegistryConfig    registryConfig;

    /**
     * The method is invoked by Java EE container automatically after registered bean definition
     */
    @PostConstruct
    public void init() {
        // Put methods to cache in order to accelerate the speed of executing.
        discoverMethods(interfaceClass);
        String name = defaultIfEmpty(beanName, buildProviderStubBeanName(interfaceName, form, version));
        // Automatically add {@link ProviderStub} instance to {@link ProviderStubHolder}
        ProviderStubHolder.getInstance().add(name, this);
    }

    /**
     * Discover all methods of provider interface and put them all to cache
     *
     * @param interfaceClass The interface class of the provider
     */
    private void discoverMethods(Class<T> interfaceClass) {
        // Get all methods of the class passed in or its super interfaces.
        Arrays.stream(interfaceClass.getMethods()).forEach(method -> {
            String methodSignature = MethodParameterUtils.getMethodSignature(method);
            methodsCache.putIfAbsent(methodSignature, method);

            List<String> methodParameters = Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.toList());
            MethodMeta methodMeta = new MethodMeta(method.getName(), methodParameters, methodSignature, method.getGenericReturnType().getTypeName());
            methodMetas.add(methodMeta);
        });
    }

    /**
     * Find method associated with name and parameters
     *
     * @param methodName       method name
     * @param methodParameters method parameters string. e.g, java.util.List,java.lang.Long
     * @return method
     */
    public Method findMethod(String methodName, String methodParameters) {
        String methodSignature = MethodParameterUtils.getMethodSignature(methodName, methodParameters);
        Method method = methodsCache.get(methodSignature);
        if (method == null) {
            method = BUILD_IN_METHODS_CACHE.get(methodSignature);
        }
        return method;
    }

    /**
     * Check health status of the service
     *
     * @return status
     */
    public String checkHealth() {
        return activated.get() ? RpcCheckHealthResponse.STATUS_OK : RpcCheckHealthResponse.STATUS_INACTIVE;
    }

    /**
     * Register the RPC providers to registries
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registryConfig    registry configuration
     */
    public void register(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig, RegistryConfig registryConfig) {
        this.applicationConfig = applicationConfig;
        this.protocolConfig = protocolConfig;
        this.registryConfig = registryConfig;

        url = createProviderUrl(applicationConfig, protocolConfig);
        // Register provider URL to all the registries
        this.registryConfig.getRegistryImpl().register(url);
    }

    public void reregister(Map<String, String> options) {
        deactivate();

        // Override the old options
        url.getOptions().clear();
        options.forEach((key, value) -> url.addOption(key, value));

        // Register provider URL to all the registries
        this.registryConfig.getRegistryImpl().register(url);
        activate();
    }

    /**
     * Deregister current RPC provider from registry
     */
    public void deregister(Url... registryUrls) {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            if (registry == null || CollectionUtils.isEmpty(registry.getRegisteredProviderUrls())) {
                log.warn("No registry found!");
                return;
            }
            registry.deregister(url);
            log.debug("Deregistered RPC provider [{}] from registry [{}]", interfaceName, registryUrl.getProtocol());
        }
    }

    /**
     * Activate the RPC providers from registries
     */
    public void activate() {
        if (activated.compareAndSet(false, true)) {
            Protocol.getInstance(url.getProtocol()).exposeProvider(url);
            this.registryConfig.getRegistryImpl().activate(url);
        }
    }

    /**
     * Deactivate the RPC providers from registries
     */
    public void deactivate() {
        if (activated.compareAndSet(true, false)) {
            Protocol.getInstance(url.getProtocol()).hideProvider(url);
            this.registryConfig.getRegistryImpl().deactivate(url);
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
        Url url = Url.providerUrl(defaultIfEmpty(protocol, ProtocolConstants.PROTOCOL_VAL_DEFAULT), protocolConfig.getHost(),
                protocolConfig.getPort(), interfaceName, form, version);
        url.addOption(ApplicationConstants.APP, applicationConfig.getId());
        url.addOption(ProtocolConstants.SERIALIZER, serializer);
        url.addOption(ProviderConstants.HEALTH_CHECKER, healthChecker);
        url.addOption(ServiceConstants.REQUEST_TIMEOUT, requestTimeout);
        url.addOption(ServiceConstants.RETRY_COUNT, retryCount);
        url.addOption(ServiceConstants.MAX_PAYLOAD, maxPayload);

        url.addOption(ProtocolConstants.CODEC, protocolConfig.getCodec());
        url.addOption(ProtocolConstants.NETWORK_TRANSMISSION, protocolConfig.getEndpointFactory());

        String minClientConn = protocolConfig.getMinClientConn() == null ? null : protocolConfig.getMinClientConn().toString();
        url.addOption(ProtocolConstants.MIN_CLIENT_CONN, minClientConn);

        String maxClientFailedConn = protocolConfig.getMaxClientFailedConn() == null ? null : protocolConfig.getMaxClientFailedConn().toString();
        url.addOption(ProtocolConstants.MAX_CLIENT_FAILED_CONN, maxClientFailedConn);

        String maxServerConn = protocolConfig.getMaxServerConn() == null ? null : protocolConfig.getMaxServerConn().toString();
        url.addOption(ProtocolConstants.MAX_SERVER_CONN, maxServerConn);

        String maxContentLength = protocolConfig.getMaxContentLength() == null ? null : protocolConfig.getMaxContentLength().toString();
        url.addOption(ProtocolConstants.MAX_CONTENT_LENGTH, maxContentLength);

        String minThread = protocolConfig.getMinThread() == null ? null : protocolConfig.getMinThread().toString();
        url.addOption(ProtocolConstants.MIN_THREAD, minThread);

        String maxThread = protocolConfig.getMaxThread() == null ? null : protocolConfig.getMaxThread().toString();
        url.addOption(ProtocolConstants.MAX_THREAD, maxThread);

        String workQueueSize = protocolConfig.getWorkQueueSize() == null ? null : protocolConfig.getWorkQueueSize().toString();
        url.addOption(ProtocolConstants.WORK_QUEUE_SIZE, workQueueSize);

        String sharedChannel = protocolConfig.getSharedChannel() == null ? null : protocolConfig.getSharedChannel().toString();
        url.addOption(ProtocolConstants.SHARED_SERVER, sharedChannel);

        String asyncInitConn = protocolConfig.getAsyncInitConn() == null ? null : protocolConfig.getAsyncInitConn().toString();
        url.addOption(ProtocolConstants.ASYNC_CREATE_CONN, asyncInitConn);

        if (MapUtils.isNotEmpty(methodConfig)) {
            for (Map.Entry<String, MethodConfig> entry : methodConfig.entrySet()) {
                for (Field field : MethodConfig.class.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        if (!Modifier.isStatic(field.getModifiers()) && field.get(entry.getValue()) != null) {
                            String name = Url.METHOD_CONFIG_PREFIX + entry.getKey() + "." + field.getName();
                            url.addOption(name, field.get(entry.getValue()).toString());
                        }
                    } catch (IllegalAccessException e) {
                        throw new RpcConfigException("Failed to read method configuration!", e);
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
    public Responseable invokeMethod(Requestable request) {
        RpcResponse response = new RpcResponse();
        Method method = findMethod(request.getMethodName(), request.getMethodParameters());
        String methodSignature = MethodParameterUtils.getFullMethodSignature(request);
        if (method == null) {
            RpcFrameworkException exception =
                    new RpcFrameworkException(methodSignature + " does NOT exist!");
            response.setException(exception);
            return response;
        }
        boolean defaultThrowExceptionStack = ProtocolConstants.TRANS_EXCEPTION_STACK_VAL_DEFAULT;
        try {
            // Invoke real method of provider
            Object result = null;
            if (BUILD_IN_METHODS.contains(request.getMethodName())) {
                result = method.invoke(this, request.getMethodArguments());
            } else if (activated.get()) {
                result = method.invoke(instance, request.getMethodArguments());
            }
            log.info("Executed method {}", methodSignature);
            response.setResult(result);
        } catch (Exception e) {
            // If exception occurs
            if (e.getCause() != null) {
                response.setException(new RpcBizException("Failed to invoke method [" + MethodParameterUtils.getMethodSignature(method) + "]", e.getCause()));
            } else {
                response.setException(new RpcBizException("Failed to invoke method [" + MethodParameterUtils.getMethodSignature(method) + "]", e));
            }

            // Do NOT print exception in error level log when exception declared in method
            boolean logException = true;
            for (Class<?> clazz : method.getExceptionTypes()) {
                if (clazz.isInstance(response.getException().getCause())) {
                    logException = false;
                    defaultThrowExceptionStack = false;
                    break;
                }
            }
            if (logException) {
                log.error("Failed to invoke method [" + MethodParameterUtils.getMethodSignature(method) + "] with request [" + request + "]", e);
            } else {
                log.info("Failed to invoke method [{}] with request [{}] and exception [{}]",
                        MethodParameterUtils.getMethodSignature(method), request, response.getException().getCause().toString());
            }
        } catch (Throwable error) {
            // Convert error to exception if error occurs
            if (error.getCause() != null) {
                response.setException(new RpcFrameworkException("Failed to invoke method [" + MethodParameterUtils.getMethodSignature(method) + "]", error.getCause()));
            } else {
                response.setException(new RpcFrameworkException("Failed to invoke method [" + MethodParameterUtils.getMethodSignature(method) + "]", error));
            }
            log.error("Failed to invoke method [" + MethodParameterUtils.getMethodSignature(method) + "] with request [" + request + "]", error);
        }
        if (response.getException() != null) {
            if (!this.url.getBooleanOption(ProtocolConstants.TRANS_EXCEPTION_STACK, defaultThrowExceptionStack)) {
                // Do NOT transport exception stack
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
     * @param interfaceName provider interface class name
     * @param form          form
     * @param version       version
     * @return provider stub bean name
     */
    public static String buildProviderStubBeanName(String interfaceName, String form, String version) {
        return ProviderStubBeanNameBuilder
                .builder(interfaceName)
                .form(form)
                .version(version)
                .build();
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
