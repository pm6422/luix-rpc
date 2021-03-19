package org.infinity.rpc.democlient.filter;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * This filter is used to calculate the execution time
 */
@Deprecated
@Slf4j
public class RequestExecutionTimeFilter implements Filter {
    private final ThreadLocal<Long> threadLocalStartTime = new ThreadLocal<>();

    @Override
    public void init(FilterConfig filterConfig) {
        // Nothing to initialize
    }

    @Override
    public void destroy() {
        // Nothing to destroy
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String method = ((HttpServletRequest) request).getMethod();
        threadLocalStartTime.set(System.currentTimeMillis());
        log.info("Processing {} request [{}] ==>==>==>==>==>==>==>==>==>==>==>==>==>==>==>==>", method,
                ((HttpServletRequest) request).getRequestURI());
        chain.doFilter(request, response);
        if (threadLocalStartTime.get() != null) {
            long executionTime = System.currentTimeMillis() - threadLocalStartTime.get();
            threadLocalStartTime.remove();
            log.info("Processed {} request [{}] <==<==<==<==<==<==<==<==<==<==<==<==<==<==<==", method,
                    ((HttpServletRequest) request).getRequestURI());
            log.info("Processed request in {} ms", executionTime);
        }
    }
}
