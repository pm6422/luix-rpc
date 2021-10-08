package org.infinity.luix.core.utils;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.constant.RpcConstants;
import org.infinity.luix.core.constant.ServiceConstants;
import org.infinity.luix.core.server.response.Responseable;
import org.infinity.luix.core.server.response.impl.RpcResponse;
import org.infinity.luix.core.switcher.impl.SwitcherHolder;
import org.infinity.luix.core.url.Url;

import static org.apache.commons.io.IOUtils.DIR_SEPARATOR_UNIX;
import static org.infinity.luix.core.constant.ProtocolConstants.*;
import static org.infinity.luix.core.constant.ProviderConstants.HEALTH_CHECKER;

public class RpcFrameworkUtils {
    /**
     * Get provider key
     *
     * @param providerUrl provider URL
     * @return provider key with format 'protocol://host:port/interface/form/version'
     */
    public static String getProviderKey(Url providerUrl) {
        return providerUrl.getProtocol() + RpcConstants.PROTOCOL_SEPARATOR + providerUrl.getAddress() + DIR_SEPARATOR_UNIX
                + providerUrl.getPath() + DIR_SEPARATOR_UNIX + providerUrl.getForm() + DIR_SEPARATOR_UNIX + providerUrl.getVersion();
    }

    /**
     * 根据Request得到 interface.method(paramDesc) 的 desc
     * <p>
     * <pre>
     * 		比如：
     * 			package com.weibo.api.motan;
     *
     * 		 	interface A { public hello(int age); }
     *
     * 			那么return "com.weibo.api.motan.A.hell(int)"
     * </pre>
     *
     * @param request
     * @return
     */
    public static String getFullMethodString(Requestable request) {
        return request.getInterfaceName() + "." + request.getMethodName() + "("
                + request.getMethodParameters() + ")";
    }

    public static String getGroupMethodString(Requestable request) {
        return getFormFromRequest(request) + "_" + getFullMethodString(request);
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

        if (!StringUtils.equals(source.getOption(SERIALIZER), target.getOption(SERIALIZER))) {
            return false;
        }

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
