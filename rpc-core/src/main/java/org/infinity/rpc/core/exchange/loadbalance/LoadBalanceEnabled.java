package org.infinity.rpc.core.exchange.loadbalance;

import org.infinity.rpc.core.exchange.Requestable;
import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

@Spi(scope = Scope.PROTOTYPE)
public interface LoadBalanceEnabled<T> {
//    void onRefresh(List<Referer<T>> referers);
//
//    Referer<T> select(Requestable request);
//
//    void selectToHolder(Requestable request, List<Referer<T>> refersHolder);
//
//    void setWeightString(String weightString);
}
