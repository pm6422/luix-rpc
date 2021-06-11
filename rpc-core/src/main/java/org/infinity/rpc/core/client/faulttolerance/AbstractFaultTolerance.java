package org.infinity.rpc.core.client.faulttolerance;

import lombok.Getter;
import lombok.Setter;
import org.infinity.rpc.core.client.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.url.Url;

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