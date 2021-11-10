package org.infinity.luix.core.client.faulttolerance;

import lombok.Getter;
import lombok.Setter;
import org.infinity.luix.core.client.loadbalancer.LoadBalancer;
import org.infinity.luix.core.url.Url;

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