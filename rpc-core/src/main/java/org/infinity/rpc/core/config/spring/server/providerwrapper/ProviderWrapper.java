package org.infinity.rpc.core.config.spring.server.providerwrapper;

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
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.MethodParameterUtils;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PRC provider configuration wrapper
 *
 * @param <T>
 */
@Slf4j
@Data
public class ProviderWrapper<T> implements DisposableBean {
    /**
     * The provider interface fully-qualified name
     */
    private String              interfaceName;
    /**
     * The interface class of the provider
     */
    private Class<T>            interfaceClass;
    /**
     * The provider instance simple name, also known as bean name
     */
    private String              instanceName;
    /**
     * The provider instance
     */
    private T                   instance;
    /**
     * Methods of the provider class
     */
    private Map<String, Method> methodsCache = new HashMap<>();
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
     * Indicator to monitor health
     */
    private boolean             checkHealth;
    /**
     * The provider url
     */
    private Url                 url;
    /**
     * Active flag
     */
    private boolean             active       = false;
    /**
     * Closed flag
     */
    private boolean             closed       = false;

    /**
     * The method is invoked by Java EE container automatically after registered bean definition
     * Automatically add {@link ProviderWrapper} instance to {@link ProviderWrapperHolder}
     */
    @PostConstruct
    public void init() {
        ProviderWrapperHolder.getInstance().addWrapper(interfaceName, this);
        scanMethods(interfaceClass);
    }

    /**
     * Get all methods of provider interface and put them all to cache
     *
     * @param interfaceClass The interface class of the provider
     */
    private void scanMethods(Class<T> interfaceClass) {
        // Get all methods of the class passed in or its super interfaces.
        Arrays.stream(interfaceClass.getMethods()).forEach(method -> {
            String methodSignature = MethodParameterUtils.getMethodSignature(method);
            methodsCache.putIfAbsent(methodSignature, method);
        });
    }

    public Method findMethod(String methodName, String methodParamList) {
        return methodsCache.get(MethodParameterUtils.getMethodSignature(methodName, methodParamList));
    }

    /**
     * todo: move to providerExecutor
     *
     * @param request
     * @return
     */
    public Responseable call(Requestable request) {
        RpcFrameworkUtils.logEvent(request, RpcConstants.TRACE_BEFORE_BIZ);
        Responseable response = invoke(request);
        RpcFrameworkUtils.logEvent(response, RpcConstants.TRACE_AFTER_BIZ);
        return response;
    }

    /**
     * todo: move to providerExecutor
     *
     * @param request
     * @return
     */
    public Responseable invoke(Requestable request) {
        RpcResponse response = new RpcResponse();
        Method method = findMethod(request.getMethodName(), request.getParameterTypeList());
        if (method == null) {
            RpcServiceException exception =
                    new RpcServiceException("Service method not exist: " + request.getInterfaceName() + "." + request.getMethodName()
                            + "(" + request.getParameterTypeList() + ")", RpcErrorMsgConstant.SERVICE_NOT_FOUND);
            response.setException(exception);
            return response;
        }

        boolean defaultThrowExceptionStack = Url.PARAM_TRANS_EXCEPTION_STACK_DEFAULT_VALUE;
        try {
            Object result = method.invoke(instance, request.getMethodArguments());
            response.setResult(result);
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
        response.setAttachments(request.getAttachments());
        return response;
    }

    /**
     * Register the RPC provider to registry
     *
     * @param app          application info
     * @param registryUrls registry urls
     * @param providerUrl  provider url
     */
    public void register(App app, List<Url> registryUrls, Url providerUrl) {
        for (Url registryUrl : registryUrls) {
            // Register provider URL to all the registries
            RegistryFactory registryFactoryImpl = RegistryFactory.getInstance(registryUrl.getProtocol());
            Registry registry = registryFactoryImpl.getRegistry(registryUrl);
            registry.register(providerUrl);
            registry.registerApplicationProvider(app, providerUrl);
        }
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

    @Override
    public void destroy() {
        active = false;
        closed = true;
    }
}
