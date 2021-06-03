package org.infinity.rpc.core.client.faulttolerance.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.faulttolerance.AbstractFaultTolerance;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.client.request.Requestable;
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
 * Failover fault tolerance strategy
 * Failover is a backup mode of operation, when the primary system exception that functions to the secondary system.
 * 失败自动切换，当出现失败会重试其它服务器
 */
@Slf4j
@SpiName(FAULT_TOLERANCE_VAL_FAILOVER)
public class FailoverFaultTolerance extends AbstractFaultTolerance {
    @Override
    public Responseable invoke(Requestable request) {
        // Select multiple nodes
        List<Sendable> availableInvokers = loadBalancer.selectSenders(request);
        // todo: provider configuration over consumer configuration
        int maxRetries = availableInvokers.get(0).getProviderUrl().getIntOption(MAX_RETRIES, MAX_RETRIES_VAL_DEFAULT);
        if (maxRetries == 0) {
            maxRetries = request.getIntOption(MAX_RETRIES, MAX_RETRIES_VAL_DEFAULT);
        }

        // Retry the RPC request operation till the max retry times
        for (int i = 0; i <= maxRetries; i++) {
            Sendable invoker = availableInvokers.get(i % availableInvokers.size());
            try {
                request.setRetryNumber(i);
                return invoker.sendRequest(request);
            } catch (RuntimeException e) {
                if (ExceptionUtils.isBizException(e) || i >= maxRetries) {
                    // Throw the exception if it's a business one
                    // Throw the exception when it exceeds the max retry times
                    throw e;
                }
                // If one of the provider nodes fails, try to use another backup available one
                log.warn(MessageFormat.format("Failed to call {0}", invoker.getProviderUrl()), e);
            }
        }
        throw new RpcFrameworkException("Failed to perform " + maxRetries + " retries to call " + availableInvokers.get(0).getProviderUrl() + "!");
    }
}
