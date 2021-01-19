package org.infinity.rpc.core.exchange.server.stub;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcBizException;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.impl.RpcResponse;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.switcher.impl.SwitcherService;
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
import java.util.List;
import java.util.Map;

/**
 * PRC provider stub
 * A stub in distributed computing is a piece of code that converts parameters passed between client and server
 * during a remote procedure call (RPC).
 * A provider stub take charge of handling remote method invocation and delegate to associate local method to execute.
 *
 * @param <T>: provider instance
 */
@Slf4j
@Data
public class ProviderStub<T> {
    /**
     * The provider interface fully-qualified name
     */
    @NotEmpty
    private String              interfaceName;
    /**
     * The interface class of the provider
     */
    @NotNull
    private Class<T>            interfaceClass;
    /**
     * Registry
     */
    @NotEmpty
    private String              registry;
    /**
     * Protocol
     */
    @NotEmpty
    private String              protocol;
    /**
     * Group
     */
    @NotEmpty
    private String              group;
    /**
     * Version
     */
    @NotEmpty
    private String              version;
    /**
     * Indicator to check health
     */
    private boolean             checkHealth;
    /**
     *
     */
    private String              checkHealthFactory;
    /**
     * The field name must be identical to the field of {@link org.infinity.rpc.core.server.annotation.Provider}
     */
    @Min(value = 0, message = "The [timeout] property of @Provider must NOT be a negative number!")
    private int                 timeout;
    /**
     * The max retry times of RPC request
     * The field name must be identical to the field of {@link org.infinity.rpc.core.server.annotation.Provider}
     */
    @Min(value = 0, message = "The [maxRetries] property of @Provider must NOT be a negative number!")
    private int                 maxRetries;
    /**
     * The provider instance
     */
    @NotNull
    private T                   instance;
    /**
     * Method signature to method cache map for the provider class
     */
    private Map<String, Method> methodsCache = new HashMap<>();
    /**
     * The provider url
     */
    private Url                 url;
    /**
     * Active flag
     */
    private boolean             active       = false;

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
     * Register the RPC provider to registry
     *
     * @param app          application info
     * @param registryUrls registry urls
     * @param providerUrl  provider url
     */
    public void register(App app, List<Url> registryUrls, Url providerUrl) {
        // Export RPC provider service
        Protocol.getInstance(providerUrl.getProtocol()).export(this);

        for (Url registryUrl : registryUrls) {
            // Register provider URL to all the registries
            RegistryFactory registryFactoryImpl = RegistryFactory.getInstance(registryUrl.getProtocol());
            Registry registry = registryFactoryImpl.getRegistry(registryUrl);
            registry.register(providerUrl);
            registry.registerApplicationProvider(app, providerUrl);
        }

        SwitcherService.getInstance().setValue(SwitcherService.REGISTRY_HEARTBEAT_SWITCHER, true);
        // Set active to true after registering the RPC provider to registry
        active = true;
        log.debug("Registered RPC provider [{}] to registry", interfaceName);
    }

    /**
     * Unregister the RPC provider from registry
     */
    public void unregister(List<Url> registryUrls) {
        // TODO: the method is never be invoked
        for (Url registryUrl : registryUrls) {
            RegistryFactory registryFactoryImpl = RegistryFactory.getInstance(registryUrl.getProtocol());
            Registry registry = registryFactoryImpl.getRegistry(registryUrl);
            if (registry == null || CollectionUtils.isEmpty(registry.getRegisteredProviderUrls())) {
                log.warn("No registry found!");
                return;
            }
            registry.getRegisteredProviderUrls().forEach(registry::unregister);
        }
        log.debug("Unregistered RPC provider [{}] from registry", interfaceName);
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
            boolean transExceptionStack = this.url.getBooleanParameter(Url.PARAM_TRANS_EXCEPTION_STACK, defaultThrowExceptionStack);
            if (!transExceptionStack) {
                //不传输业务异常栈
                ExceptionUtils.setMockStackTrace(response.getException().getCause());
            }
        }
        response.setOptions(request.getOptions());
        return response;
    }

//    @Override
//    public void destroy() {
//        active = false;
//    }
}
