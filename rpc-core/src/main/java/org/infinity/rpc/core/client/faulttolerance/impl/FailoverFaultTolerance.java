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
import static org.infinity.rpc.core.constant.ServiceConstants.RETRY_COUNT;
import static org.infinity.rpc.core.constant.ServiceConstants.RETRY_COUNT_VAL_DEFAULT;

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
        int retryCount;
        if (StringUtils.isNotEmpty(request.getOption(RETRY_COUNT))) {
            retryCount = request.getIntOption(RETRY_COUNT);
        } else {
            // Get method level parameter value
            retryCount = allActiveSenders.get(0).getProviderUrl()
                    .getMethodParameter(request.getMethodName(), request.getMethodParameters(),
                            RETRY_COUNT, RETRY_COUNT_VAL_DEFAULT);
        }

        // Retry the RPC request operation till the max retry times
        for (int i = 0; i <= retryCount; i++) {
            Sendable sender = allActiveSenders.get(i % allActiveSenders.size());
            try {
                request.setRetryNumber(i);
                // Send RPC request
                return sender.sendRequest(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e) || i >= retryCount) {
                    // Throw the exception if it's a business one
                    // Throw the exception when it exceeds the max retry times
                    throw e;
                }
                // If one of the provider nodes fails, try to use another backup available one
                log.warn(MessageFormat.format("Failed to call {0}", sender.getProviderUrl()), e);
            }
        }
        throw new RpcFrameworkException("Failed to perform " + retryCount + " retries to call " + allActiveSenders.get(0).getProviderUrl() + "!");
    }
}
