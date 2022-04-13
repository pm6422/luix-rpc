package com.luixtech.luixrpc.core.client.faulttolerance;

import lombok.Getter;
import lombok.Setter;
import com.luixtech.luixrpc.core.client.loadbalancer.LoadBalancer;
import com.luixtech.luixrpc.core.url.Url;

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