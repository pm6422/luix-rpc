package org.infinity.rpc.core.exchange.request.impl;

import lombok.Data;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.Map;

@ThreadSafe
@Data
public class RequestContext {
    /**
     * Create a new {@link RequestContext} for each thread
     */
    private static final ThreadLocal<RequestContext> THREAT_LOCAL_CONTEXT = ThreadLocal.withInitial(RequestContext::new);
    private              String                      clientRequestId;
    private              Requestable                 request;
    private              boolean                     asyncCall            = false;
    private              Responseable                response;
    /**
     * RPC context attachment. not same as request attachments
     */
    private              Map<String, String>         attachments          = new HashMap<>();
    private              Map<Object, Object>         attributes           = new HashMap<>();

    /**
     * Prohibit instantiate an instance outside the class
     */
    private RequestContext() {
    }

    public static RequestContext getThreadRpcContext() {
        return THREAT_LOCAL_CONTEXT.get();
    }

    public static void destroy() {
        THREAT_LOCAL_CONTEXT.remove();
    }

    /**
     * init new rpcContext with request
     *
     * @param request
     * @return
     */
    public static RequestContext initialize(Requestable request) {
        RequestContext context = new RequestContext();
        if (request != null) {
            context.setRequest(request);
            String clientRequestId = request.getClientRequestId();
            context.setClientRequestId(clientRequestId);
        }
        THREAT_LOCAL_CONTEXT.set(context);
        return context;
    }

    public static RequestContext initialize() {
        RequestContext context = new RequestContext();
        THREAT_LOCAL_CONTEXT.set(context);
        return context;
    }

    public String getRequestId() {
        if (clientRequestId != null) {
            return clientRequestId;
        }
        return request == null ? null : String.valueOf(request.getRequestId());
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

    public void addAttachment(String key, String value) {
        attachments.put(key, value);
    }

    public String getAttachment(String key) {
        return attachments.get(key);
    }

    public void removeAttachment(String key) {
        attachments.remove(key);
    }
}
