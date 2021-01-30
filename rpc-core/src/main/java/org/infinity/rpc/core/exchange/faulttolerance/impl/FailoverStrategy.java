package org.infinity.rpc.core.exchange.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.faulttolerance.AbstractFaultToleranceStrategy;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.text.MessageFormat;
import java.util.List;

import static org.infinity.rpc.core.constant.ServiceConstants.MAX_RETRIES;
import static org.infinity.rpc.core.constant.ServiceConstants.MAX_RETRIES_DEFAULT_VALUE;

/**
 * Failover fault tolerance strategy
 * Failover is a backup mode of operation, when the primary system exception that functions to the secondary system.
 *
 * @param <T>: The interface class of the provider
 */
@Slf4j
@ServiceName("failover")
public class FailoverStrategy<T> extends AbstractFaultToleranceStrategy<T> {
    @Override
    public Responseable call(LoadBalancer<T> loadBalancer, Requestable request) {
        // Select multiple nodes
        List<ProviderCaller<T>> availableProviderCallers = loadBalancer.selectProviderNodes(request);
        int maxRetries = availableProviderCallers.get(0).getProviderUrl().getIntOption(MAX_RETRIES, MAX_RETRIES_DEFAULT_VALUE);

        // Retry the RPC request operation till the max retry times
        for (int i = 0; i <= maxRetries; i++) {
            ProviderCaller<T> providerCaller = availableProviderCallers.get(i % availableProviderCallers.size());
            try {
                request.setRetryNumber(i);
                return providerCaller.call(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e) || i >= maxRetries) {
                    // Throw the exception if it's a business one
                    // Throw the exception when it exceeds the max retry times
                    throw e;
                }
                // If one of the provider nodes fails, try to use another backup available one
                // todo: refactor the message
                log.warn(MessageFormat.format("Failed to call the provider: {0}", providerCaller.getProviderUrl()), e);
            }
        }
        // todo: refactor the message
        throw new RpcFrameworkException("Failed to call all the providers!");
    }
}
