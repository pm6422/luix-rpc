package org.infinity.rpc.core.exchange.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.faulttolerance.AbstractFaultToleranceStrategy;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.text.MessageFormat;
import java.util.List;

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
        List<ProviderRequester<T>> availableProviderRequesters = loadBalancer.selectProviderNodes(request);
        int maxRetries = availableProviderRequesters.get(0).getProviderUrl().getIntParameter(Url.PARAM_MAX_RETRIES);

        // Retry the RPC request operation till the max retry times
        for (int i = 0; i <= maxRetries; i++) {
            ProviderRequester<T> providerRequester = availableProviderRequesters.get(i % availableProviderRequesters.size());
            try {
                request.setNumberOfRetry(i);
                return providerRequester.call(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e) || i >= maxRetries) {
                    // Throw the exception if it's a business one
                    // Throw the exception when it exceeds the max retry times
                    throw e;
                }
                // If one of the provider nodes fails, try to use another backup available one
                // todo: refactor the message
                log.warn(MessageFormat.format("Failed to call the url: {0}", providerRequester.getProviderUrl()), e);
            }
        }
        // todo: refactor the message
        throw new RpcFrameworkException("Failed to call all the urls!");
    }
}
