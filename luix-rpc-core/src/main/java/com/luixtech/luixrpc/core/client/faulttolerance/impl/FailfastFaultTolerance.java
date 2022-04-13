package com.luixtech.luixrpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.client.request.Requestable;
import com.luixtech.luixrpc.core.constant.ConsumerConstants;
import com.luixtech.luixrpc.core.server.response.Responseable;
import com.luixtech.luixrpc.core.client.faulttolerance.AbstractFaultTolerance;
import com.luixtech.luixrpc.core.client.sender.Sendable;
import com.luixtech.luixrpc.utilities.serviceloader.annotation.SpiName;

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
