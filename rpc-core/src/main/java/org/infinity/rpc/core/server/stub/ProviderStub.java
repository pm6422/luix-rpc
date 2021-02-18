package org.infinity.rpc.core.server.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.config.ProviderConfig;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcBizException;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.MethodParameterUtils;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.infinity.rpc.core.constant.ProtocolConstants.LOCAL_ADDRESS_FACTORY;
import static org.infinity.rpc.core.constant.ServiceConstants.*;

/**
 * PRC provider stub
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
     * The provider interface fully-qualified name
     */
    @NotEmpty(message = "The [interfaceName] property of @Consumer must NOT be empty!")
    private           String              interfaceName;
    /**
     * The interface class of the provider
     */
    @NotNull(message = "The [interfaceClass] property of @Consumer must NOT be null!")
    private           Class<T>            interfaceClass;
    /**
     * Protocol
     */
    private           String              protocol;
    /**
     * Registry
     */
    private           String              registry;
    /**
     * Group
     */
    private           String              group;
    /**
     * Version
     */
    private           String              version;
    /**
     * Indicator to check health
     * Note: It must be specified with Boolean wrapper class
     */
    private           Boolean             checkHealth;
    /**
     *
     */
    private           String              checkHealthFactory;
    /**
     * The field name must be identical to the field of {@link org.infinity.rpc.core.server.annotation.Provider}
     */
    @Min(value = 0, message = "The [timeout] property of @Provider must NOT be a negative number!")
    private           int                 requestTimeout;
    /**
     * The max retry times of RPC request
     * The field name must be identical to the field of {@link org.infinity.rpc.core.server.annotation.Provider}
     */
    @Min(value = 0, message = "The [maxRetries] property of @Provider must NOT be a negative number!")
    private           int                 maxRetries;
    /**
     * The provider instance
     * Disable serialize
     */
    @NotNull
    private transient T                   instance;
    /**
     * Method signature to method cache map for the provider class
     */
    private transient Map<String, Method> methodsCache = new HashMap<>();
    /**
     * The provider url
     */
    private           Url                 url;
    /**
     * Active flag
     */
    private           boolean             active       = false;

    /**
     * The method is invoked by Java EE container automatically after registered bean definition
     * Automatically add {@link ProviderStub} instance to {@link ProviderStubHolder}
     */
    @PostConstruct
    public void init() {
        ProviderStubHolder.getInstance().addStub(interfaceName, this);
        // Put methods to cache in order to accelerate the speed of executing.
        discoverMethods(interfaceClass);
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
        });
    }

    /**
     * Find method associated with name and parameter list
     *
     * @param methodName       method name
     * @param methodParameters method parameter list. e.g, java.util.List,java.lang.Long
     * @return method
     */
    public Method findMethod(String methodName, String methodParameters) {
        return methodsCache.get(MethodParameterUtils.getMethodSignature(methodName, methodParameters));
    }

    /**
     * Register the RPC providers to registries
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registryConfig    registry configuration
     * @param providerConfig    provider configuration
     */
    public void register(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig,
                         RegistryConfig registryConfig, ProviderConfig providerConfig) {
        // Export provider url
        Url providerUrl = createProviderUrl(applicationConfig, protocolConfig, registryConfig, providerConfig);

        // Export RPC provider service
        Protocol.getInstance(providerUrl.getProtocol()).export(this);

        // Register provider URL to all the registries
        registryConfig.getRegistryImpl().register(providerUrl);
//            registry.registerApplicationProvider(applicationConfig.getName(), providerUrl);
        log.debug("Registered RPC provider [{}] to registry [{}]", interfaceName,
                registryConfig.getRegistryImpl().getRegistryUrl().getProtocol());

        // Set active to true after registering the RPC provider to registry
        active = true;
    }

    /**
     * Merge high priority properties to provider stub and generate provider url
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registryConfig    registry configuration
     * @param providerConfig    provider configuration
     * @return provider url
     */
    private Url createProviderUrl(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig,
                                  RegistryConfig registryConfig, ProviderConfig providerConfig) {
        if (StringUtils.isEmpty(protocol)) {
            protocol = protocolConfig.getName();
        }
        if (StringUtils.isEmpty(registry)) {
            registry = registryConfig.getName();
        }
        if (StringUtils.isEmpty(group)) {
            group = providerConfig.getGroup();
        }
        if (StringUtils.isEmpty(version)) {
            version = providerConfig.getVersion();
        }

        url = Url.providerUrl(protocol, protocolConfig.getHost(), protocolConfig.getPort(), interfaceName, group, version);
        url.addOption(Url.PARAM_APP, applicationConfig.getName());
        url.addOption(LOCAL_ADDRESS_FACTORY, protocolConfig.getLocalAddressFactory());

        if (checkHealth == null) {
            checkHealth = providerConfig.isCheckHealth();
        }
        url.addOption(CHECK_HEALTH, String.valueOf(checkHealth));

        if (StringUtils.isEmpty(checkHealthFactory)) {
            checkHealthFactory = providerConfig.getCheckHealthFactory();
        }
        url.addOption(CHECK_HEALTH_FACTORY, checkHealthFactory);

        if (Integer.MAX_VALUE == requestTimeout) {
            requestTimeout = providerConfig.getRequestTimeout();
        }
        url.addOption(REQUEST_TIMEOUT, String.valueOf(requestTimeout));

        if (Integer.MAX_VALUE == maxRetries) {
            maxRetries = providerConfig.getMaxRetries();
        }
        url.addOption(MAX_RETRIES, String.valueOf(maxRetries));

        return url;
    }

    /**
     * Unregister the RPC provider from registry
     */
    public void unregister(Url... registryUrls) {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            if (registry == null || CollectionUtils.isEmpty(registry.getRegisteredProviderUrls())) {
                log.warn("No registry found!");
                return;
            }
            registry.getRegisteredProviderUrls().forEach(registry::unregister);
            log.debug("Unregistered RPC provider [{}] from registry [{}]", interfaceName, registryUrl.getProtocol());
        }
    }

    /**
     * Invoke method locally and return the result
     *
     * @param request RPC request
     * @return RPC response
     */
    public Responseable localCall(Requestable request) {
        RpcFrameworkUtils.logEvent(request, RpcConstants.TRACE_BEFORE_BIZ);
        Responseable response = doLocalCall(request);
        RpcFrameworkUtils.logEvent(response, RpcConstants.TRACE_AFTER_BIZ);
        return response;
    }

    /**
     * Do the invocation of method
     *
     * @param request RPC request
     * @return RPC response
     */
    private Responseable doLocalCall(Requestable request) {
        RpcResponse response = new RpcResponse();
        Method method = findMethod(request.getMethodName(), request.getMethodParameters());
        if (method == null) {
            RpcServiceException exception =
                    new RpcServiceException("Service method not exist: " + request.getInterfaceName() + "." + request.getMethodName()
                            + "(" + request.getMethodParameters() + ")", RpcErrorMsgConstant.SERVICE_NOT_FOUND);
            response.setException(exception);
            return response;
        }
        boolean defaultThrowExceptionStack = Url.PARAM_TRANS_EXCEPTION_STACK_DEFAULT_VALUE;
        try {
            // Invoke method
            Object result = method.invoke(instance, request.getMethodArguments());
            response.setResultObject(result);
        } catch (Exception e) {
            if (e.getCause() != null) {
                response.setException(new RpcBizException("provider call process error", e.getCause()));
            } else {
                response.setException(new RpcBizException("provider call process error", e));
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
                log.error("Exception caught when during method invocation. request:" + request.toString(), e);
            } else {
                log.info("Exception caught when during method invocation. request:" + request.toString() + ", exception:" + response.getException().getCause().toString());
            }
        } catch (Throwable t) {
            // 如果服务发生Error，将Error转化为Exception，防止拖垮调用方
            if (t.getCause() != null) {
                response.setException(new RpcServiceException("provider has encountered a fatal error!", t.getCause()));
            } else {
                response.setException(new RpcServiceException("provider has encountered a fatal error!", t));
            }
            //对于Throwable,也记录日志
            log.error("Exception caught when during method invocation. request:" + request.toString(), t);
        }
        if (response.getException() != null) {
            //是否传输业务异常栈
            boolean transExceptionStack = this.url.getBooleanOption(Url.PARAM_TRANS_EXCEPTION_STACK, defaultThrowExceptionStack);
            if (!transExceptionStack) {
                //不传输业务异常栈
                ExceptionUtils.setMockStackTrace(response.getException().getCause());
            }
        }
        response.setOptions(request.getOptions());
        return response;
    }
}
