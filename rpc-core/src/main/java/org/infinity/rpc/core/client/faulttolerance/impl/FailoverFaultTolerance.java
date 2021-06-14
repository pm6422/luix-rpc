package org.infinity.rpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import java.text.MessageFormat;
import java.util.List;

import static org.infinity.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_FAILOVER;
import static org.infinity.rpc.core.constant.ServiceConstants.MAX_RETRIES;
import static org.infinity.rpc.core.constant.ServiceConstants.MAX_RETRIES_VAL_DEFAULT;

/**
 * Failover is a backup mode of operation, when the primary system exception that functions to the secondary system.
 * Failover fault tolerance strategy means that when a service node fails to call, it will continue to call other service nodes.
 */
@Slf4j
@SpiName(FAULT_TOLERANCE_VAL_FAILOVER)
public class FailoverFaultTolerance extends AbstractFaultTolerance {
    @Override
    public Responseable invoke(Requestable request) {
        // Select all active senders
        List<Sendable> allActiveSenders = loadBalancer.selectAllActiveSenders(request);
        int maxRetries;
        if (StringUtils.isNotEmpty(request.getOption(MAX_RETRIES))) {
            maxRetries = request.getIntOption(MAX_RETRIES);
        } else {
            // Get method level parameter value
            maxRetries = allActiveSenders.get(0).getProviderUrl()
                    .getMethodParameter(request.getMethodName(), request.getMethodParameters(),
                            MAX_RETRIES, MAX_RETRIES_VAL_DEFAULT);
        }

        // Retry the RPC request operation till the max retry times
        for (int i = 0; i <= maxRetries; i++) {
            Sendable sender = allActiveSenders.get(i % allActiveSenders.size());
            try {
                request.setRetryNumber(i);
                // Send RPC request
                return sender.sendRequest(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e) || i >= maxRetries) {
                    // Throw the exception if it's a business one
                    // Throw the exception when it exceeds the max retry times
                    throw e;
                }
                // If one of the provider nodes fails, try to use another backup available one
                log.warn(MessageFormat.format("Failed to call {0}", sender.getProviderUrl()), e);
            }
        }
        throw new RpcFrameworkException("Failed to perform " + maxRetries + " retries to call " + allActiveSenders.get(0).getProviderUrl() + "!");
    }
}
