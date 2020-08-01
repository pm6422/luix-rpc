package org.infinity.rpc.core.exchange.ha;

import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

@Spi(scope = Scope.PROTOTYPE)
public interface HighAvailability<T> {

    void setClientUrl(Url clientUrl);

    Url getClientUrl();

    Responseable<T> call(Requestable<T> request, LoadBalancer<T> loadBalancer);
}