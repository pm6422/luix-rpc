package org.infinity.rpc.core.exchange.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.faulttolerance.AbstractFaultToleranceStrategy;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.ExceptionUtils;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.text.MessageFormat;
import java.util.List;

/**
 * Failover fault tolerance strategy
 * "failover" is a backup mode of operation, when the primary system exception that functions to the secondary system.
 *
 * @param <T>: The interface class of the provider
 */
@Slf4j
@ServiceName("failover")
public class FailoverStrategy<T> extends AbstractFaultToleranceStrategy<T> {
    @Override
    public Responseable call(LoadBalancer<T> loadBalancer, Requestable request) {
        // Select more than one nodes
        List<ProviderRequester<T>> availableProviderRequesters = loadBalancer.selectProviderNodes(request);
        int maxRetries = 1;
        // TODO: get retry per provider configuration
        Url url = availableProviderRequesters.get(0).getProviderUrl();
//        int tryCount = url.getMethodParameter(request.getMethodName(),
//                request.getParamtersDesc(),
//                UrlParam.retries.getName(),
//                UrlParam.retries.getIntValue());

        for (int i = 0; i <= maxRetries; i++) {
            ProviderRequester<T> providerRequester = availableProviderRequesters.get(i % availableProviderRequesters.size());
            try {
                request.setRetries(i);
                return providerRequester.call(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e)) {
                    // Throw the exception when it's a business one
                    throw e;
                } else if (i >= maxRetries) {
                    // Throw the exception when it exceeds the max retries count
                    throw e;
                }
                // If one of the nodes fails, try to use another backup available one
                log.warn(MessageFormat.format("Failed to call the url: {0}", providerRequester.getProviderUrl()), e);
            }
        }
        throw new RpcFrameworkException("Failed to call all the urls!");
    }
}
