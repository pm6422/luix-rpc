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

package org.infinity.rpc.core.config.spring.server.messagehandler.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.config.spring.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.config.spring.server.stub.ProviderStub;
import org.infinity.rpc.core.exception.RpcBizException;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.impl.RpcRequest;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.serialization.DeserializableObject;
import org.infinity.rpc.core.exchange.transport.Channel;
import org.infinity.rpc.core.utils.MethodParameterUtils;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * service 消息处理
 * <p>
 * <pre>
 * 		1） 多个service的支持
 * 		2） 区分service的方式： group/interface/version
 * </pre>
 */
@Slf4j
public class ProviderMessageRouter implements MessageHandler {
    protected Map<String, ProviderStub<?>> providers = new HashMap<>();

    /**
     * 所有暴露出去的方法计数
     * 比如：messageRouter 里面涉及2个Service: ServiceA 有5个public method，ServiceB
     * 有10个public method，那么就是15
     */
    protected AtomicInteger methodCounter = new AtomicInteger(0);

    public ProviderMessageRouter() {
    }

    public ProviderMessageRouter(ProviderStub<?> provider) {
        addProvider(provider);
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
        String serviceKey = RpcFrameworkUtils.getServiceKey(request);
        ProviderStub<?> provider = providers.get(serviceKey);

        if (provider == null) {
            log.error(this.getClass().getSimpleName() + " handler Error: provider not exist serviceKey=" + serviceKey + " " + request);
            RpcServiceException exception =
                    new RpcServiceException(this.getClass().getSimpleName() + " handler Error: provider not exist serviceKey="
                            + serviceKey + " " + request);

            return RpcFrameworkUtils.buildErrorResponse(request, exception);
        }
        Method method = provider.findMethod(request.getMethodName(), request.getMethodParameters());
        fillParamDesc(request, method);
        processLazyDeserialize(request, method);
        Responseable response = call(request, provider);
        response.setSerializeNum(request.getSerializeNum());
        response.setProtocolVersion(request.getProtocolVersion());
        return response;
    }

    protected Responseable call(Requestable request, ProviderStub<?> provider) {
        try {
            return provider.localCall(request);
        } catch (Exception e) {
            return RpcFrameworkUtils.buildErrorResponse(request, new RpcBizException("provider call process error", e));
        }
    }

    private void processLazyDeserialize(Requestable request, Method method) {
        if (method != null && request.getMethodArguments() != null && request.getMethodArguments().length == 1
                && request.getMethodArguments()[0] instanceof DeserializableObject
                && request instanceof RpcRequest) {
            try {
                Object[] args = ((DeserializableObject) request.getMethodArguments()[0]).deserializeMulti(method.getParameterTypes());
                ((RpcRequest) request).setMethodArguments(args);
            } catch (IOException e) {
                throw new RpcFrameworkException("deserialize parameters fail: " + request.toString() + ", error:" + e.getMessage());
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

    public synchronized void addProvider(ProviderStub<?> provider) {
        String serviceKey = RpcFrameworkUtils.getServiceKey(provider.getUrl());
        if (providers.containsKey(serviceKey)) {
            throw new RpcFrameworkException("Provider already exists with the key [" + serviceKey + "]");
        }

        providers.put(serviceKey, provider);

        // 获取该service暴露的方法数：
        List<Method> methods = MethodParameterUtils.getPublicMethod(provider.getInterfaceClass());
        //todo
//        CompressRpcCodec.putMethodSign(provider, methods);// 对所有接口方法生成方法签名。适配方法签名压缩调用方式。

        int publicMethodCount = methods.size();
        methodCounter.addAndGet(publicMethodCount);

        log.info("RequestRouter addProvider: url=" + provider.getUrl() + " all_public_method_count=" + methodCounter.get());
    }

    public synchronized void removeProvider(ProviderStub<?> provider) {
        String serviceKey = RpcFrameworkUtils.getServiceKey(provider.getUrl());

        providers.remove(serviceKey);
        List<Method> methods = MethodParameterUtils.getPublicMethod(provider.getInterfaceClass());
        int publicMethodCount = methods.size();
        methodCounter.getAndSet(methodCounter.get() - publicMethodCount);

        log.info("RequestRouter removeProvider: url=" + provider.getUrl() + " all_public_method_count=" + methodCounter.get());
    }

    public int getPublicMethodCount() {
        return methodCounter.get();
    }
}
