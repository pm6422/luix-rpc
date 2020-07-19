package org.infinity.rpc.core.client.proxy;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exchange.Requestable;
import org.infinity.rpc.core.exchange.RpcExchangeContext;
import org.infinity.rpc.core.registry.UrlParam;
import org.infinity.rpc.core.switcher.SwitcherService;

import java.util.Map;

public abstract class AbstractRpcConsumerInvocationHandler<T> {
    protected String          interfaceName;
    protected Class<T>        clazz;
    protected SwitcherService switcherService;

    protected void initialize() {

    }

    protected Object invokeRequest(Requestable request, Class returnType, boolean async) throws Throwable {
        RpcExchangeContext context = RpcExchangeContext.getContext();
        context.addAttribute(RpcConstants.ASYNC_SUFFIX, async);

        // set rpc context attachments to request
        Map<String, String> attachments = context.getAttachments();
        if (MapUtils.isNotEmpty(attachments)) {
            for (Map.Entry<String, String> entry : attachments.entrySet()) {
                request.attachment(entry.getKey(), entry.getValue());
            }
        }

        // add to attachment if client request id is set
        if (StringUtils.isNotBlank(context.getClientRequestId())) {
            request.attachment(UrlParam.requestIdFromClient.getName(), context.getClientRequestId());
        }

        return null;
    }

}
