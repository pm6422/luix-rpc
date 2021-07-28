package org.infinity.rpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import java.text.MessageFormat;
import java.util.List;

import static org.infinity.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_BROADCAST;

@Slf4j
@SpiName(FAULT_TOLERANCE_VAL_BROADCAST)
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
