package com.luixtech.luixrpc.core.utils;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.luixrpc.core.client.request.Requestable;
import com.luixtech.luixrpc.core.constant.RpcConstants;
import com.luixtech.luixrpc.core.constant.ServiceConstants;
import com.luixtech.luixrpc.core.server.response.Responseable;
import com.luixtech.luixrpc.core.server.response.impl.RpcResponse;
import com.luixtech.luixrpc.core.switcher.impl.SwitcherHolder;
import com.luixtech.luixrpc.core.url.Url;

import static org.apache.commons.io.IOUtils.DIR_SEPARATOR_UNIX;
import static com.luixtech.luixrpc.core.constant.ProtocolConstants.*;
import static com.luixtech.luixrpc.core.constant.ProviderConstants.HEALTH_CHECKER;
import static com.luixtech.luixrpc.core.url.Url.PROTOCOL_SEPARATOR;

public class RpcFrameworkUtils {
    /**
     * Get provider key
     *
     * @param providerUrl provider URL
     * @return provider key with format 'protocol://host:port/interface/form/version'
     */
    public static String getProviderKey(Url providerUrl) {
        return providerUrl.getProtocol() + PROTOCOL_SEPARATOR + providerUrl.getAddress() + DIR_SEPARATOR_UNIX
                + providerUrl.getPath() + DIR_SEPARATOR_UNIX + providerUrl.getForm() + DIR_SEPARATOR_UNIX + providerUrl.getVersion();
    }

    public static String getMethodKey(String protocol, String interfaceName, String methodName, String form, String version) {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append(PROTOCOL_SEPARATOR);
        if (StringUtils.isNotEmpty(form)) {
            sb.append(form).append(DIR_SEPARATOR_UNIX);
        }
        if (StringUtils.isNotEmpty(version)) {
            sb.append(version).append(DIR_SEPARATOR_UNIX);
        }
        sb.append(interfaceName).append(".").append(methodName).append("()");
        return sb.toString();
    }

    /**
     * 判断url:source和url:target是否可以使用共享的service channel(port) 对外提供服务
     * <p>
     * <pre>
     * 		1） protocol
     * 		2） codec
     * 		3） serialize
     * 		4） maxContentLength
     * 		5） maxServerConnection
     * 		6） maxWorkerThread
     * 		7） workerQueueSize
     * 		8） heartbeatFactory
     * </pre>
     *
     * @param source
     * @param target
     * @return
     */
    public static boolean checkIfCanShareServiceChannel(Url source, Url target) {
        if (!StringUtils.equals(source.getProtocol(), target.getProtocol())) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(CODEC), target.getOption(CODEC))) {
            return false;
        }

//        if (!StringUtils.equals(source.getOption(SERIALIZER), target.getOption(SERIALIZER))) {
//            return false;
//        }

        if (!StringUtils.equals(source.getOption(MAX_CONTENT_LENGTH), target.getOption(MAX_CONTENT_LENGTH))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(MAX_SERVER_CONN), target.getOption(MAX_SERVER_CONN))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(MAX_THREAD), target.getOption(MAX_THREAD))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(WORK_QUEUE_SIZE), target.getOption(WORK_QUEUE_SIZE))) {
            return false;
        }

        return StringUtils.equals(source.getOption(HEALTH_CHECKER), target.getOption(HEALTH_CHECKER));
    }

    /**
     * 判断url:source和url:target是否可以使用共享的client channel(port) 对外提供服务
     * <p>
     * <pre>
     * 		1） protocol
     * 		2） codec
     * 		3） serialize
     * 		4） maxContentLength
     * 		5） maxClientConnection
     * 		6） heartbeatFactory
     * </pre>
     *
     * @param source
     * @param target
     * @return
     */
    public static boolean checkIfCanShallClientChannel(Url source, Url target) {
        if (!StringUtils.equals(source.getProtocol(), target.getProtocol())) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(CODEC), target.getOption(CODEC))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(SERIALIZER), target.getOption(SERIALIZER))) {
            return false;
        }

        if (!StringUtils.equals(source.getOption(MAX_CONTENT_LENGTH), target.getOption(MAX_CONTENT_LENGTH))) {
            return false;
        }

//        if (!StringUtils.equals(source.getOption(MAX_CLIENT_CONN), target.getOption(MAX_CLIENT_CONN))) {
//            return false;
//        }

        return StringUtils.equals(source.getOption(HEALTH_CHECKER), target.getOption(HEALTH_CHECKER));

    }

    public static String getFormFromRequest(Requestable request) {
        return getValueFromRequest(request, ServiceConstants.FORM);
    }

    public static String getVersionFromRequest(Requestable request) {
        return getValueFromRequest(request, ServiceConstants.VERSION);
    }

    public static String getValueFromRequest(Requestable request, String key) {
        return MapUtils.isNotEmpty(request.getOptions()) ? request.getOption(key) : null;
    }

    /**
     * 获取默认motan协议配置
     *
     * @return motan协议配置
     */
//    public static ProtocolConfig getDefaultProtocolConfig() {
//        ProtocolConfig pc = new ProtocolConfig();
//        pc.setId("motan");
//        pc.setName("motan");
//        return pc;
//    }

    /**
     * 默认本地注册中心
     *
     * @return local registry
     */
//    public static RegistryConfig getDefaultRegistryConfig() {
//        RegistryConfig local = new RegistryConfig();
//        local.setRegProtocol("local");
//        return local;
//    }
    public static RpcResponse buildErrorResponse(Requestable request, Exception e) {
        return buildErrorResponse(request.getRequestId(), request.getProtocolVersion(), e);
    }

    public static RpcResponse buildErrorResponse(long requestId, byte version, Exception e) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setProtocolVersion(version);
        response.setException(e);
        return response;
    }

    public static void logEvent(Requestable request, String event) {
        if (SwitcherHolder.getInstance().isOn(RpcConstants.MOTAN_TRACE_INFO_SWITCHER, false)) {
            logEvent(request, event, System.currentTimeMillis());
        }
    }

    public static void logEvent(Requestable request, String event, long time) {
        if (request == null) {
            return;
        }
        if (RpcConstants.TRACE_CSEND.equals(event)) {
            request.setSendingTime(time);
            return;
        }
        if (RpcConstants.TRACE_SRECEIVE.equals(event)) {
            request.setReceivedTime(time);
            return;
        }
        if (SwitcherHolder.getInstance().isOn(RpcConstants.MOTAN_TRACE_INFO_SWITCHER, false)) {
            request.addTrace(event, String.valueOf(time));
        }
    }

    public static void logEvent(Responseable response, String event) {
        if (SwitcherHolder.getInstance().isOn(RpcConstants.MOTAN_TRACE_INFO_SWITCHER, false)) {
            logEvent(response, event, System.currentTimeMillis());
        }
    }

    public static void logEvent(Responseable response, String event, long time) {
        if (response == null) {
            return;
        }
        if (RpcConstants.TRACE_SSEND.equals(event)) {
            response.setSendingTime(time);
            return;
        }
        if (RpcConstants.TRACE_CRECEIVE.equals(event)) {
            response.setReceivedTime(time);
            return;
        }
        if (SwitcherHolder.getInstance().isOn(RpcConstants.MOTAN_TRACE_INFO_SWITCHER, false)) {
            response.addTrace(event, String.valueOf(time));
        }
    }
}
