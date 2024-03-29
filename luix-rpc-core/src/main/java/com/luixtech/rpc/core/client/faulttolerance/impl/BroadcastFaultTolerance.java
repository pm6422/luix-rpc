package com.luixtech.rpc.core.client.faulttolerance.impl;

import com.luixtech.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.client.sender.Sendable;
import com.luixtech.rpc.core.constant.ConsumerConstants;
import com.luixtech.rpc.core.exception.ExceptionUtils;
import com.luixtech.rpc.core.server.response.Responseable;
import com.luixtech.utilities.serviceloader.annotation.SpiName;
import lombok.extern.slf4j.Slf4j;

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
