package org.infinity.rpc.core.client.invoker.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invoker.ServiceInvoker;
import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import static org.infinity.rpc.core.constant.ConsumerConstants.CLUSTER_VAL_P2P;

/**
 * It means that only one service provider can handle one request
 * todo: ClusterSpi
 */
@Slf4j
@SpiName(CLUSTER_VAL_P2P)
@Setter
@Getter
public class P2pServiceInvoker implements ServiceInvoker {
    private boolean        active = false;
    private String         interfaceName;
    private FaultTolerance faultTolerance;

    @Override
    public void init() {
        active = true;
        ShutdownHook.add(this::destroy);
    }

    @Override
    public void destroy() {
        active = false;
        faultTolerance.destroy();
    }

    @Override
    public Responseable invoke(Requestable request) {
        if (!active) {
            throw new RpcFrameworkException("No active service [" + toString() + "] found!");
        }
        return faultTolerance.invoke(request);
    }

    @Override
    public String toString() {
        if (StringUtils.isEmpty(interfaceName)) {
            return P2pServiceInvoker.class.getSimpleName();
        }
        return P2pServiceInvoker.class.getSimpleName().concat(":").concat(interfaceName);
    }
}
