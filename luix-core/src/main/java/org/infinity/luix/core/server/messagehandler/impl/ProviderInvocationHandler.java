/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.infinity.luix.core.server.messagehandler.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.client.request.impl.RpcRequest;
import org.infinity.luix.core.constant.RpcConstants;
import org.infinity.luix.core.constant.ServiceConstants;
import org.infinity.luix.core.exception.impl.RpcBizException;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.Channel;
import org.infinity.luix.core.server.messagehandler.MessageHandler;
import org.infinity.luix.core.server.response.Responseable;
import org.infinity.luix.core.server.stub.ProviderStub;
import org.infinity.luix.core.server.stub.ProviderStubHolder;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.MethodParameterUtils;
import org.infinity.luix.core.utils.RpcFrameworkUtils;
import org.infinity.luix.utilities.serializer.DeserializableArgs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ProviderInvocationHandler implements MessageHandler {
    /**
     * Map of provider stub name to provider URL
     */
    protected static final Map<String, Url> NAME_2_URL            = new ConcurrentHashMap<>();
    /**
     * The count of exported service methods
     */
    protected static final AtomicInteger    EXPORTED_METHOD_COUNT = new AtomicInteger(0);

    public ProviderInvocationHandler() {
    }

    @Override
    public Object handle(Channel channel, Object message) {
        if (channel == null || message == null) {
            throw new RpcFrameworkException("RequestRouter handler(channel, message) params is null");
        }
        if (!(message instanceof Requestable)) {
            throw new RpcFrameworkException("RequestRouter message type not support: " + message.getClass());
        }

        Requestable request = (Requestable) message;
        String stubName = ProviderStub.buildProviderStubBeanName(request.getInterfaceName(),
                request.getOption(ServiceConstants.FORM), request.getOption(ServiceConstants.VERSION));
        ProviderStub<?> providerStub = ProviderStubHolder.getInstance().get().get(stubName);

        if (providerStub == null) {
            log.error("No provider found with key [{}] for {}", stubName, request);
            RpcFrameworkException exception = new RpcFrameworkException("No provider found with key [" + stubName + "] for " + request);
            return RpcFrameworkUtils.buildErrorResponse(request, exception);
        }
        Method method = providerStub.findMethod(request.getMethodName(), request.getMethodParameters());
        fillParamDesc(request, method);
        // Process arguments
        processLazyDeserialize(request, method);
        Responseable response = invoke(request, providerStub);
        // Set serializer ID of response with request's one
        response.setSerializerId(request.getSerializerId());
        response.setProtocolVersion(request.getProtocolVersion());
        return response;
    }

    protected Responseable invoke(Requestable request, ProviderStub<?> providerStub) {
        try {
            RpcFrameworkUtils.logEvent(request, RpcConstants.TRACE_BEFORE_BIZ);
            Responseable response = providerStub.invokeMethod(request);
            RpcFrameworkUtils.logEvent(response, RpcConstants.TRACE_AFTER_BIZ);
            return response;
        } catch (Exception e) {
            return RpcFrameworkUtils.buildErrorResponse(request, new RpcBizException("Failed to call provider", e));
        }
    }

    private void processLazyDeserialize(Requestable request, Method method) {
        if (method != null && request.getMethodArguments() != null && request.getMethodArguments().length == 1
                && request.getMethodArguments()[0] instanceof DeserializableArgs
                && request instanceof RpcRequest) {
            try {
                Object[] args = ((DeserializableArgs) request.getMethodArguments()[0]).deserialize(method.getParameterTypes());
                ((RpcRequest) request).setMethodArguments(args);
            } catch (IOException e) {
                throw new RpcFrameworkException("deserialize parameters fail: " + request + ", error:" + e.getMessage());
            }
        }
    }

    private void fillParamDesc(Requestable request, Method method) {
        if (method != null && StringUtils.isBlank(request.getMethodParameters()) && request instanceof RpcRequest) {
            RpcRequest dr = (RpcRequest) request;
            dr.setMethodParameters(MethodParameterUtils.getMethodParameters(method));
            dr.setMethodName(method.getName());
        }
    }

    public synchronized void addProvider(Url providerUrl) {
        String stubName = ProviderStub.buildProviderStubBeanName(providerUrl.getPath(),
                providerUrl.getForm(), providerUrl.getVersion());
        ProviderStub<?> providerStub = ProviderStubHolder.getInstance().get().get(stubName);

        if (NAME_2_URL.containsKey(stubName)) {
            throw new RpcFrameworkException("Provider already exists with the key [" + stubName + "]");
        }
        NAME_2_URL.put(stubName, providerUrl);

        List<Method> methods = MethodParameterUtils.getPublicMethod(providerStub.getInterfaceClass());
        //todo
//        CompressRpcCodec.putMethodSign(provider, methods);// 对所有接口方法生成方法签名。适配方法签名压缩调用方式。
        // Calculate the total of exported public methods
        EXPORTED_METHOD_COUNT.addAndGet(methods.size());
        log.info("Added service provider [{}] to router", providerUrl);
    }

    public synchronized void removeProvider(Url providerUrl) {
        String stubName = ProviderStub.buildProviderStubBeanName(providerUrl.getPath(),
                providerUrl.getForm(), providerUrl.getVersion());
        ProviderStub<?> providerStub = ProviderStubHolder.getInstance().get().get(stubName);

        NAME_2_URL.remove(stubName);

        List<Method> methods = MethodParameterUtils.getPublicMethod(providerStub.getInterfaceClass());
        EXPORTED_METHOD_COUNT.getAndSet(EXPORTED_METHOD_COUNT.get() - methods.size());
        log.info("Removed service provider [{}] from router", providerUrl);
    }

    public int getPublicMethodCount() {
        return EXPORTED_METHOD_COUNT.get();
    }
}
