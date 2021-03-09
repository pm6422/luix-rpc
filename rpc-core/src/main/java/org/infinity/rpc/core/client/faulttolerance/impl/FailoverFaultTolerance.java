package org.infinity.rpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import org.infinity.rpc.core.client.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.client.request.Invokable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import java.text.MessageFormat;
import java.util.List;

import static org.infinity.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_FAILOVER;
import static org.infinity.rpc.core.constant.ServiceConstants.MAX_RETRIES;
import static org.infinity.rpc.core.constant.ServiceConstants.MAX_RETRIES_VAL_DEFAULT;

/**
 * Failover fault tolerance strategy
 * Failover is a backup mode of operation, when the primary system exception that functions to the secondary system.
 */
@Slf4j
@SpiName(FAULT_TOLERANCE_VAL_FAILOVER)
public class FailoverFaultTolerance extends AbstractFaultTolerance {
    @Override
    public Responseable call(Requestable request, LoadBalancer loadBalancer) {
        // Select multiple nodes
        List<Invokable> availableImporters = loadBalancer.selectProviderNodes(request);
        // todo: provider configuration over consumer configuration
        int maxRetries = availableImporters.get(0).getProviderUrl().getIntOption(MAX_RETRIES, MAX_RETRIES_VAL_DEFAULT);
        if (maxRetries == 0) {
            maxRetries = request.getIntOption(MAX_RETRIES, MAX_RETRIES_VAL_DEFAULT);
        }

        // Retry the RPC request operation till the max retry times
        for (int i = 0; i <= maxRetries; i++) {
            Invokable importer = availableImporters.get(i % availableImporters.size());
            try {
                request.setRetryNumber(i);
                return importer.call(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e) || i >= maxRetries) {
                    // Throw the exception if it's a business one
                    // Throw the exception when it exceeds the max retry times
                    throw e;
                }
                // If one of the provider nodes fails, try to use another backup available one
                log.warn(MessageFormat.format("Failed to call {0}", importer.getProviderUrl()), e);
            }
        }
        throw new RpcFrameworkException("Failed to perform " + maxRetries + " retries to call " + availableImporters.get(0).getProviderUrl() + "!");
    }
}
