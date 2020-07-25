package org.infinity.rpc.core.exchange.ha.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.ha.AbstractHighAvailability;
import org.infinity.rpc.core.exchange.loadbalance.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

/**
 * Fail-fast fault tolerance high availability mechanism
 * to see is the "fail-fast" from the literal meaning, found errors in the system as much as possible,
 * so that the system can be pre-configured in accordance with the process execution error,
 * the corresponding mode is "fault-tolerant (fault tolerance)"
 * With JAVA collection fast failure, for example,
 * when multiple threads are operating on the same set of content, it may generate fail-fast event.
 * For example: When a thread A iterator to traverse through a collection process,
 * the content if the set is changed by the other thread;
 * then thread A collection of access,
 * ConcurrentModificationException will throw an exception (error found a good set execution error process),
 * resulting in fail-fast event.
 *
 * @param <T>
 */
@Slf4j
@ServiceName("failfast")
public class FailfastHighAvailability<T> extends AbstractHighAvailability<T> {
    @Override
    public Responseable call(Requestable request, LoadBalancer<T> loadBalancer) {
        Requester<T> availableRequester = loadBalancer.selectNode(request);
        return availableRequester.call(request);
    }
}
