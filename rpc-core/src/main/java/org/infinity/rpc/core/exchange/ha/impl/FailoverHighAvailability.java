package org.infinity.rpc.core.exchange.ha.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.ha.AbstractHighAvailability;
import org.infinity.rpc.core.exchange.loadbalance.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.utils.ExceptionUtils;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.List;

/**
 * Failover fault tolerance high availability mechanism
 * "failover" is a backup mode of operation, when the main component exception that functions to the backup components.
 *
 * @param <T>
 */
@Slf4j
@ServiceName("failover")
public class FailoverHighAvailability<T> extends AbstractHighAvailability<T> {
    @Override
    public Responseable call(Requestable request, LoadBalancer<T> loadBalancer) {
        // Select more than one nodes
        List<Requester<T>> requesters = loadBalancer.selectNodes(request);
        Url url = requesters.get(0).getUrl();
        int maxRetriesCount = 0;
        // TODO
//        int tryCount = url.getMethodParameter(request.getMethodName(),
//                request.getParamtersDesc(),
//                UrlParam.retries.getName(),
//                UrlParam.retries.getIntValue());

        if (maxRetriesCount < 0) {
            throw new RpcConfigurationException("Retries can NOT be a negative number!");
        }
        for (int i = 0; i <= maxRetriesCount; i++) {
            try {
                Requester<T> requester = requesters.get(i % requesters.size());
                request.retries(i);
                return requester.call(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e)) {
                    // Throw the exception when it's a business one
                    throw e;
                } else if (i >= maxRetriesCount) {
                    // Throw the exception when it exceeds the max retries count
                    throw e;
                }
                // If one of the nodes fails, try to use another backup available one
                log.warn(String.format("FailoverHaStrategy Call false for request:%s error=%s", request, e.getMessage()));
            }
        }
        // TODO: check remove
        throw new RpcFrameworkException("FailoverHaStrategy.call should not come here!");
    }
}
