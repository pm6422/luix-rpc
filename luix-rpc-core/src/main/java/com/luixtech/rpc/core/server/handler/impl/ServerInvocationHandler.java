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

package com.luixtech.rpc.core.server.handler.impl;

import com.luixtech.rpc.core.utils.MethodParameterUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.client.request.impl.RpcRequest;
import com.luixtech.rpc.core.constant.RpcConstants;
import com.luixtech.rpc.core.constant.ServiceConstants;
import com.luixtech.rpc.core.exception.impl.RpcBizException;
import com.luixtech.rpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.rpc.core.exchange.Channel;
import com.luixtech.rpc.core.server.handler.InvocationHandleable;
import com.luixtech.rpc.core.server.response.Responseable;
import com.luixtech.rpc.core.server.stub.ProviderStub;
import com.luixtech.rpc.core.server.stub.ProviderStubHolder;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.core.utils.RpcFrameworkUtils;
import com.luixtech.utilities.serializer.DeserializableArgs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ServerInvocationHandler implements InvocationHandleable {
    /**
     * Map of provider stub name to provider URL
     * Note: it is useless for now
     */
    protected static final Map<String, Url> NAME_2_URL           = new ConcurrentHashMap<>();
    /**
     * The count of exposed service methods
     */
    protected static final AtomicInteger    EXPOSED_METHOD_COUNT = new AtomicInteger(0);

    @Override
    public Object handle(Channel channel, Object message) {
        if (channel == null) {
            throw new RpcFrameworkException("Argument channel must NOT be null!");
        }
        if (message == null) {
            throw new RpcFrameworkException("Argument message must NOT be null!");
        }
        if (!(message instanceof Requestable)) {
            throw new RpcFrameworkException("Unsupported message type: " + message.getClass());
        }

        Requestable request = (Requestable) message;
        String stubName = ProviderStub.buildProviderStubBeanName(request.getInterfaceName(),
                request.getOption(ServiceConstants.FORM), request.getOption(ServiceConstants.VERSION));
        ProviderStub<?> providerStub = ProviderStubHolder.getInstance().getMap().get(stubName);

        if (providerStub == null) {
            log.error("No provider found with key [{}] for {}", stubName, request);
            RpcFrameworkException exception = new RpcFrameworkException("No provider found with key [" + stubName + "] for " + request);
            return RpcFrameworkUtils.buildErrorResponse(request, exception);
        }

        Method method = providerStub.findMethod(request.getMethodName(), request.getMethodParameters());
        fillMethodParameters(request, method);
        // Process lazy arguments
        deserializeLazyArgs(request, method);
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

    private void deserializeLazyArgs(Requestable request, Method method) {
        if (method != null && request.getMethodArguments() != null && request.getMethodArguments().length == 1
                && request.getMethodArguments()[0] instanceof DeserializableArgs
                && request instanceof RpcRequest) {
            try {
                Object[] args = ((DeserializableArgs) request.getMethodArguments()[0]).deserialize(method.getParameterTypes());
                ((RpcRequest) request).setMethodArguments(args);
            } catch (IOException e) {
                throw new RpcFrameworkException("Failed to deserialize arguments for request: " + request + " with error: " + e.getMessage());
            }
        }
    }

    private void fillMethodParameters(Requestable request, Method method) {
        if (method != null && StringUtils.isBlank(request.getMethodParameters()) && request instanceof RpcRequest) {
            RpcRequest rpcRequest = (RpcRequest) request;
            rpcRequest.setMethodName(method.getName());
            rpcRequest.setMethodParameters(MethodParameterUtils.getMethodParameters(method));
        }
    }

    public synchronized void addProvider(Url providerUrl) {
        String stubName = ProviderStub.buildProviderStubBeanName(providerUrl.getPath(),
                providerUrl.getForm(), providerUrl.getVersion());
        ProviderStub<?> providerStub = ProviderStubHolder.getInstance().getMap().get(stubName);

        if (NAME_2_URL.containsKey(stubName)) {
            throw new RpcFrameworkException("Provider already exists with the key [" + stubName + "]");
        }
        NAME_2_URL.put(stubName, providerUrl);

        List<Method> methods = MethodParameterUtils.getPublicMethod(providerStub.getInterfaceClass());
        // Calculate the total count of exposed public methods
        EXPOSED_METHOD_COUNT.addAndGet(methods.size());
        log.info("Added service provider [{}] to router", providerUrl);
    }

    public synchronized void removeProvider(Url providerUrl) {
        String stubName = ProviderStub.buildProviderStubBeanName(providerUrl.getPath(),
                providerUrl.getForm(), providerUrl.getVersion());
        ProviderStub<?> providerStub = ProviderStubHolder.getInstance().getMap().get(stubName);

        NAME_2_URL.remove(stubName);

        List<Method> methods = MethodParameterUtils.getPublicMethod(providerStub.getInterfaceClass());
        EXPOSED_METHOD_COUNT.getAndSet(EXPOSED_METHOD_COUNT.get() - methods.size());
        log.info("Removed service provider [{}] from router", providerUrl);
    }

    public int getPublicMethodCount() {
        return EXPOSED_METHOD_COUNT.get();
    }
}
