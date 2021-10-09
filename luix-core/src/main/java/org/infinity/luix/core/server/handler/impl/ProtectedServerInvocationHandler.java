package org.infinity.luix.core.server.handler.impl;


import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.server.response.Responseable;
import org.infinity.luix.core.server.response.impl.RpcResponse;
import org.infinity.luix.core.server.stub.ProviderStub;
import org.infinity.luix.core.utils.MethodParameterUtils;
import org.infinity.luix.core.utils.RpcFrameworkUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * provider消息处理分发：支持一定程度的自我防护
 * <p>
 * <pre>
 * 	1) 如果接口只有一个方法，那么直接return true
 * 	2) 如果接口有多个方法，那么如果单个method超过 maxThread / 2 && totalCount >  (maxThread * 3 / 4)，那么return false;
 * 	3) 如果接口有多个方法(4个)，同时总的请求数超过 maxThread * 3 / 4，同时该method的请求数超过 maxThead * 1 / 4， 那么return false
 * 	4) 其他场景return true
 * </pre>
 */
@Slf4j
public class ProtectedServerInvocationHandler extends ServerInvocationHandler {
    protected ConcurrentMap<String, AtomicInteger> requestCounters = new ConcurrentHashMap<>();
    protected AtomicInteger                        totalCounter    = new AtomicInteger(0);
    protected AtomicInteger                        rejectCounter   = new AtomicInteger(0);

    @Override
    protected Responseable invoke(Requestable request, ProviderStub<?> providerStub) {
        // 支持的最大worker thread数
        int maxThread = providerStub.getUrl().getIntOption(ProtocolConstants.MAX_THREAD, ProtocolConstants.MAX_THREAD_VAL_DEFAULT);
        String requestKey = MethodParameterUtils.getFullMethodSignature(request);

        try {
            int requestCounter = incrRequestCounter(requestKey);
            int totalCounter = incrTotalCounter();
            if (isAllowRequest(requestCounter, totalCounter, maxThread, request)) {
                return super.invoke(request, providerStub);
            } else {
                // reject request
                return reject(request.getInterfaceName() + "." + request.getMethodName(), requestCounter, totalCounter, maxThread, request);
            }
        } finally {
            decrTotalCounter();
            decrRequestCounter(requestKey);
        }
    }

    private Responseable reject(String method, int requestCounter, int totalCounter, int maxThread, Requestable request) {
        RpcFrameworkException exception = new RpcFrameworkException("ThreadProtectedRequestRouter reject request: request_counter=" + requestCounter
                + " total_counter=" + totalCounter + " max_thread=" + maxThread);
        exception.setStackTrace(new StackTraceElement[0]);
        RpcResponse response = RpcFrameworkUtils.buildErrorResponse(request, exception);
        log.error("ThreadProtectedRequestRouter reject request: request_method=" + method + " request_counter=" + requestCounter
                + " =" + totalCounter + " max_thread=" + maxThread);
        rejectCounter.incrementAndGet();
        return response;
    }

    private int incrRequestCounter(String requestKey) {
        AtomicInteger counter = requestCounters.get(requestKey);

        if (counter == null) {
            counter = new AtomicInteger(0);
            requestCounters.putIfAbsent(requestKey, counter);
            counter = requestCounters.get(requestKey);
        }

        return counter.incrementAndGet();
    }

    private int decrRequestCounter(String requestKey) {
        AtomicInteger counter = requestCounters.get(requestKey);

        if (counter == null) {
            return 0;
        }
        return counter.decrementAndGet();
    }

    private int incrTotalCounter() {
        return totalCounter.incrementAndGet();
    }

    private int decrTotalCounter() {
        return totalCounter.decrementAndGet();
    }

    protected boolean isAllowRequest(int requestCounter, int totalCounter, int maxThread, Requestable request) {
        if (EXPOSED_METHOD_COUNT.get() == 1) {
            return true;
        }

        // 该方法第一次请求，直接return true
        if (requestCounter == 1) {
            return true;
        }

        // 不简单判断 requestCount > (maxThread / 2) ，因为假如有2或者3个method对外提供，
        // 但是只有一个接口很大调用量，而其他接口很空闲，那么这个时候允许单个method的极限到 maxThread * 3 / 4
        if (requestCounter > (maxThread / 2) && totalCounter > (maxThread * 3 / 4)) {
            return false;
        }

        // 如果总体线程数超过 maxThread * 3 / 4个，并且对外的method比较多，那么意味着这个时候整体压力比较大，
        // 那么这个时候如果单method超过 maxThread * 1 / 4，那么reject
        return !(EXPOSED_METHOD_COUNT.get() >= 4 && totalCounter > (maxThread * 3 / 4) && requestCounter > (maxThread / 4));
    }

//    @Override
//    public String statisticCallback() {
//        int count = rejectCounter.getAndSet(0);
//        if (count > 0) {
//            return String.format("type: infinity name: reject_request total_count: %s reject_count: %s", totalCounter.get(), count);
//        } else {
//            return null;
//        }
//    }
}
