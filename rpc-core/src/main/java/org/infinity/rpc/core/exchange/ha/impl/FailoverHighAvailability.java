package org.infinity.rpc.core.exchange.ha.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.ha.AbstractHighAvailability;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.ExceptionUtils;
import org.infinity.rpc.utilities.spi.annotation.NameAs;

import java.text.MessageFormat;
import java.util.List;

/**
 * Failover fault tolerance high availability mechanism
 * "failover" is a backup mode of operation, when the main component exception that functions to the backup components.
 *
 * @param <T>
 */
@Slf4j
@NameAs("failover")
public class FailoverHighAvailability<T> extends AbstractHighAvailability<T> {
    @Override
    public Responseable call(Requestable<T> request, LoadBalancer<T> loadBalancer) {
        // Select more than one nodes
        List<Requester<T>> availableRequesters = loadBalancer.selectNodes(request);
        Url url = availableRequesters.get(0).getProviderUrl();
        int maxRetries = 1;
        // TODO
//        int tryCount = url.getMethodParameter(request.getMethodName(),
//                request.getParamtersDesc(),
//                UrlParam.retries.getName(),
//                UrlParam.retries.getIntValue());

        if (maxRetries < 0) {
            // TODO: move to retries parameter setting validation part
            throw new RpcConfigurationException("Retries can NOT be a negative number!");
        }
        for (int i = 0; i <= maxRetries; i++) {
            Requester<T> requester = availableRequesters.get(i % availableRequesters.size());
            try {
                request.retries(i);
                return requester.call(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e)) {
                    // Throw the exception when it's a business one
                    throw e;
                } else if (i >= maxRetries) {
                    // Throw the exception when it exceeds the max retries count
                    throw e;
                }
                // If one of the nodes fails, try to use another backup available one
                log.warn(MessageFormat.format("Failed to call the url: {0}", requester.getProviderUrl()), e);
            }
        }
        throw new RpcFrameworkException("Failed to call all the urls!");
    }
}
