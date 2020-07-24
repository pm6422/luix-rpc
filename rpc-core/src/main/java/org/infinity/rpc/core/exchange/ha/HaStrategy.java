package org.infinity.rpc.core.exchange.ha;

import org.infinity.rpc.core.exchange.loadbalance.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

@Spi(scope = Scope.PROTOTYPE)
public interface HaStrategy<T> {

    void setUrl(Url url);

    Responseable call(Requestable request, LoadBalancer<T> loadBalancer);
}