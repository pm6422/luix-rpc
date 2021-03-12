package org.infinity.rpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import org.infinity.rpc.core.client.request.Invokable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import static org.infinity.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_FAILFAST;

/**
 * Fail-fast fault tolerance strategy
 * 自动降级也即"Fail Fast(快速失败)"是通过主动或者被动探测的方式，发现服务异常后自动降级，
 * 在降级期间所有请求全部直接打回，直到服务恢复后再打开开关。
 * 自动降级的优点是反映灵敏，当服务出现问题后秒级即可感知，可确保快速将异常服务降级，
 * 缺点则是实现机制较复杂，同时容易有误伤的情况。
 */
@Slf4j
@SpiName(FAULT_TOLERANCE_VAL_FAILFAST)
public class FailfastFaultTolerance extends AbstractFaultTolerance {
    @Override
    public Responseable invoke(Requestable request) {
        Invokable availableInvoker = loadBalancer.selectProviderNode(request);
        // Do NOT retry when exception occurred
        return availableInvoker.invoke(request);
    }
}
