package org.infinity.rpc.core.exchange;

import lombok.Getter;
import lombok.Setter;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.Map;

/**
 * 感觉鸡肋，没啥用，只有在日志trace输出才有些用，而且response都没有存储值
 */
@ThreadSafe
@Setter
@Getter
@Deprecated
public class RpcContext {
    /**
     * Create a new {@link RpcContext} for each thread
     */
    private static final ThreadLocal<RpcContext> THREAT_LOCAL_CONTEXT = ThreadLocal.withInitial(RpcContext::new);
    private              long                    requestId;
    private              Requestable             request;
    private              Responseable            response;
    /**
     * Options is a container used to store {@link String} types
     */
    private              Map<String, String>     options              = new HashMap<>();
    /**
     * Attributes is a container used to store {@link Object} types
     */
    private              Map<Object, Object>     attributes           = new HashMap<>();

    /**
     * Prevent instantiation of it outside the class
     */
    private RpcContext() {
    }

    public static RpcContext getInstance() {
        return THREAT_LOCAL_CONTEXT.get();
    }

    /**
     * Initialize new rpcContext with request
     *
     * @param request RPC request
     * @return RPC context
     */
    public static RpcContext init(Requestable request) {
        RpcContext context = new RpcContext();
        if (request != null) {
            context.setRequest(request);
        }
        THREAT_LOCAL_CONTEXT.set(context);
        return context;
    }

    public void addOption(String key, String value) {
        options.put(key, value);
    }

    public String getOption(String key) {
        return options.get(key);
    }

    public void removeOption(String key) {
        options.remove(key);
    }

    public void addAttribute(Object key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(Object key) {
        return attributes.get(key);
    }

    public void removeAttribute(Object key) {
        attributes.remove(key);
    }

    public static void destroy() {
        THREAT_LOCAL_CONTEXT.remove();
    }
}
