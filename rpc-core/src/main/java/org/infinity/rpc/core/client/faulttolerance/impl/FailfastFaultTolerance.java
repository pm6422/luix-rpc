package org.infinity.rpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import org.infinity.rpc.core.client.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.client.request.ProviderCaller;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

/**
 * Fail-fast fault tolerance strategy
 * 自动降级也即"Fail Fast(快速失败)"是通过主动或者被动探测的方式，发现服务异常后自动降级，
 * 在降级期间所有请求全部直接打回，直到服务恢复后再打开开关。
 * 自动降级的优点是反映灵敏，当服务出现问题后秒级即可感知，可确保快速将异常服务降级，
 * 缺点则是实现机制较复杂，同时容易有误伤的情况。
 *
 * @param <T>: The interface class of the provider
 */
@Slf4j
@ServiceName("failfast")
public class FailfastFaultTolerance<T> extends AbstractFaultTolerance<T> {
    @Override
    public Responseable call(Requestable request, LoadBalancer<T> loadBalancer) {
        ProviderCaller<T> availableProviderCaller = loadBalancer.selectProviderNode(request);
        // Do NOT retry when exception occurred
        return availableProviderCaller.call(request);
    }
}
