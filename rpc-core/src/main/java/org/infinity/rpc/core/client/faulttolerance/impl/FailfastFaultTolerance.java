package org.infinity.rpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import static org.infinity.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_FAILFAST;

/**
 * Fail-fast tolerance strategy means that only one call is initiated,
 * and an error is reported immediately if a failure occurs.
 * It is usually used for non-idempotent operations.
 */
@Slf4j
@SpiName(FAULT_TOLERANCE_VAL_FAILFAST)
public class FailfastFaultTolerance extends AbstractFaultTolerance {
    @Override
    public Responseable invoke(Requestable request) {
        Sendable availableSender = loadBalancer.selectSender(request);
        // Do NOT retry when exception occurred
        return availableSender.sendRequest(request);
    }
}
