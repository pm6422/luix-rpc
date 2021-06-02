package org.infinity.rpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import static org.infinity.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_FAILFAST;

/**
 * Fail-fast fault tolerance strategy
 * 快速失败，只发起一次调用，失败立即报错，通常用于非幂等性操作
 */
@Slf4j
@SpiName(FAULT_TOLERANCE_VAL_FAILFAST)
public class FailfastFaultTolerance extends AbstractFaultTolerance {
    @Override
    public Responseable invoke(Requestable request) {
        Sendable availableInvoker = loadBalancer.selectProviderNode(request);
        // Do NOT retry when exception occurred
        return availableInvoker.sendRequest(request);
    }
}
