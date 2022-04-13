package com.luixtech.rpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.constant.ConsumerConstants;
import com.luixtech.rpc.core.server.response.Responseable;
import com.luixtech.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import com.luixtech.rpc.core.client.sender.Sendable;
import com.luixtech.utilities.serviceloader.annotation.SpiName;

/**
 * Fail-fast tolerance strategy means that only one call is initiated,
 * and an error is reported immediately if a failure occurs.
 * It is usually used for non-idempotent operations.
 */
@Slf4j
@SpiName(ConsumerConstants.FAULT_TOLERANCE_VAL_FAILFAST)
public class FailfastFaultTolerance extends AbstractFaultTolerance {
    @Override
    public Responseable invoke(Requestable request) {
        Sendable activeSender = loadBalancer.selectActiveSender(request);
        // Send RPC request and do NOT retry when exception occurred
        return activeSender.sendRequest(request);
    }
}
