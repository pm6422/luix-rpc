package org.infinity.rpc.core.exchange.ha.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.ha.AbstractHaStrategy;
import org.infinity.rpc.core.exchange.loadbalance.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.utils.ExceptionUtils;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

@Slf4j
@ServiceName("failover")
public class FailoverHaStrategy<T> extends AbstractHaStrategy<T> {
    @Override
    public Responseable call(Requestable request, LoadBalancer<T> loadBalancer) {
        Requester<T> requester = loadBalancer.selectNode(request);
        Url url = requester.getUrl();
        int tryCount = 0;
        // TODO
//        int tryCount = url.getMethodParameter(request.getMethodName(),
//                request.getParamtersDesc(),
//                UrlParam.retries.getName(),
//                UrlParam.retries.getIntValue());

        if (tryCount < 0) {
            tryCount = 0;
        }

        for (int i = 0; i <= tryCount; i++) {
            try {
                request.retries(i);
                return requester.call(request);
            } catch (RuntimeException e) {
                // 对于业务异常，直接抛出
                if (ExceptionUtils.isBizException(e)) {
                    throw e;
                } else if (i >= tryCount) {
                    throw e;
                }
                log.warn(String.format("FailoverHaStrategy Call false for request:%s error=%s", request, e.getMessage()));
            }
        }
        // TODO: check remove
        throw new RpcFrameworkException("FailoverHaStrategy.call should not come here!");
    }
}
