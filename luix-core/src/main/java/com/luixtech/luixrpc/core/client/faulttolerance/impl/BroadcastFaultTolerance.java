package com.luixtech.luixrpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.client.request.Requestable;
import com.luixtech.luixrpc.core.constant.ConsumerConstants;
import com.luixtech.luixrpc.core.server.response.Responseable;
import com.luixtech.luixrpc.core.client.faulttolerance.AbstractFaultTolerance;
import com.luixtech.luixrpc.core.client.sender.Sendable;
import com.luixtech.luixrpc.core.exception.ExceptionUtils;
import com.luixtech.luixrpc.utilities.serviceloader.annotation.SpiName;

import java.text.MessageFormat;
import java.util.List;

@Slf4j
@SpiName(ConsumerConstants.FAULT_TOLERANCE_VAL_BROADCAST)
public class BroadcastFaultTolerance extends AbstractFaultTolerance {
    @Override
    public Responseable invoke(Requestable request) {
        // Select all active senders
        List<Sendable> allActiveSenders = loadBalancer.selectAllActiveSenders(request);

        Responseable response = null;
        for (Sendable sender : allActiveSenders) {
            try {
                // Send RPC request
                response = sender.sendRequest(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e)) {
                    // Throw the exception if it's a business one
                    // Throw the exception when it exceeds the max retry times
                    throw e;
                }
                log.warn(MessageFormat.format("Failed to call {0}", sender.getProviderUrl()), e);
            }
        }
        // Return the last response
        return response;
    }
}
