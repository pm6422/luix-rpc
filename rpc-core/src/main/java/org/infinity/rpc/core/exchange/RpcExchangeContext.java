package org.infinity.rpc.core.exchange;

import lombok.Data;
import org.infinity.rpc.core.registry.UrlParam;

import java.util.HashMap;
import java.util.Map;

@Data
public class RpcExchangeContext {
    private static final ThreadLocal<RpcExchangeContext> LOCAL_CONTEXT = ThreadLocal.withInitial(() -> new RpcExchangeContext());
    private              String                          clientRequestId;
    private              Requestable                     request;
    private              Responseable                    response;
    /**
     * RPC context attachment. not same as request attachments
     */
    private              Map<String, String>             attachments   = new HashMap<>();
    private              Map<Object, Object>             attributes    = new HashMap<>();

    public static RpcExchangeContext getContext() {
        return LOCAL_CONTEXT.get();
    }

    public static void destroy() {
        LOCAL_CONTEXT.remove();
    }

    /**
     * init new rpcContext with request
     *
     * @param request
     * @return
     */
    public static RpcExchangeContext initialize(Requestable request) {
        RpcExchangeContext context = new RpcExchangeContext();
        if (request != null) {
            context.setRequest(request);
            String clientRequestId = request.getAttachment(UrlParam.requestIdFromClient.getName());
            context.setClientRequestId(clientRequestId);
        }
        LOCAL_CONTEXT.set(context);
        return context;
    }

    public static RpcExchangeContext initialize() {
        RpcExchangeContext context = new RpcExchangeContext();
        LOCAL_CONTEXT.set(context);
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
