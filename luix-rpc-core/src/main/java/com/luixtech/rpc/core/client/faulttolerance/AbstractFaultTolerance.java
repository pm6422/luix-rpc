package com.luixtech.rpc.core.client.faulttolerance;

import com.luixtech.rpc.core.client.loadbalancer.LoadBalancer;
import com.luixtech.rpc.core.url.Url;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class AbstractFaultTolerance implements FaultTolerance {
    protected Url          consumerUrl;
    protected LoadBalancer loadBalancer;

    public AbstractFaultTolerance() {
        super();
    }

    @Override
    public void destroy() {
        loadBalancer.destroy();
    }
}