package org.infinity.rpc.democlient.component;

import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.interceptor.Interceptor;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.democlient.utils.TraceIdUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * It used to add trace ID to HTTP header when initiating a HTTP request by forest
 */
@Component
@Slf4j
public class TraceInterceptor implements Interceptor<String> {
    /**
     * Execute before call
     *
     * @param request forest request
     * @return {@code true} if it continue to execute and {@code false} otherwise
     */
    @Override
    public boolean beforeExecute(ForestRequest request) {
        Optional.ofNullable(TraceIdUtils.getTraceId()).ifPresent(traceId -> request.addHeader(TraceIdUtils.TRACE_ID, traceId));
        return true;
    }
}
