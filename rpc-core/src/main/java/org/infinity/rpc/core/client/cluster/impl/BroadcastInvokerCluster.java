package org.infinity.rpc.core.client.cluster.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.cluster.InvokerCluster;
import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import static org.infinity.rpc.core.constant.ConsumerConstants.CLUSTER_VAL_BROADCAST;

@Slf4j
@SpiName(CLUSTER_VAL_BROADCAST)
@Setter
@Getter
public class BroadcastInvokerCluster implements InvokerCluster {
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
