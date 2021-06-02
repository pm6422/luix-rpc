package org.infinity.rpc.core.client.invoker.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.invoker.ServiceInvoker;
import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import static org.infinity.rpc.core.constant.ConsumerConstants.CLUSTER_VAL_BROADCAST;

/**
 * It means to handle one request by all the service providers
 */
@Slf4j
@SpiName(CLUSTER_VAL_BROADCAST)
@Setter
@Getter
public class BroadcastServiceInvoker implements ServiceInvoker {
    @Override
    public void init() {

    }

    @Override
    public Responseable invoke(Requestable request) {
        return null;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void setInterfaceName(String interfaceName) {

    }

    @Override
    public void setFaultTolerance(FaultTolerance faultTolerance) {

    }

    @Override
    public FaultTolerance getFaultTolerance() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
