package com.luixtech.rpc.core.client.request.impl;

import com.luixtech.rpc.core.protocol.constants.ProtocolVersion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.rpc.core.client.request.Requestable;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@ToString
public class RpcRequest implements Requestable, Serializable {
    private static final long                serialVersionUID = -6259178379027752471L;
    protected            long                requestId;
    protected            String              protocol;
    /**
     * todo: check usage
     */
    protected            byte                protocolVersion  = ProtocolVersion.VERSION_1.getVersion();
    protected            String              interfaceName;
    protected            String              methodName;
    /**
     * The method parameter type name list string which is separated by comma.
     * e.g, java.util.List,java.lang.Long
     */
    protected            String              methodParameters;
    protected            Object[]            methodArguments;
    protected            boolean             async            = false;
    protected            int                 retryNumber;
    protected            long                sendingTime;
    protected            long                receivedTime;
    protected            long                elapsedTime;
    protected            Map<String, String> traces           = new ConcurrentHashMap<>();
    /**
     * RPC request options, all the optional RPC request parameters will be put in it.
     */
    protected            Map<String, String> options          = new ConcurrentHashMap<>();
    /**
     * Default serialization is Hessian2
     */
    protected            int                 serializerId;

    public RpcRequest(long requestId, String protocol, String interfaceName,
                      String methodName, String methodParameters, boolean async) {
        this.requestId = requestId;
        this.protocol = protocol;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.methodParameters = methodParameters;
        this.async = async;
    }

    @Override
    public void addOption(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return;
        }
        options.putIfAbsent(key, value);
    }

    @Override
    public void addOption(String key, Integer value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return;
        }
        options.putIfAbsent(key, value.toString());
    }

    public void addOption(String key, Integer value, int defaultValue) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        if (value != null) {
            options.putIfAbsent(key, value.toString());
        } else {
            options.putIfAbsent(key, String.valueOf(defaultValue));
        }
    }

    @Override
    public String getOption(String key) {
        return options.get(key);
    }

    @Override
    public String getOption(String key, String defaultValue) {
        String value = getOption(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public int getIntOption(String key) {
        return Integer.parseInt(options.get(key));
    }

    @Override
    public int getIntOption(String key, int defaultValue) {
        String value = getOption(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    @Override
    public void addTrace(String key, String value) {
        traces.putIfAbsent(key, value);
    }

    @Override
    public String getTrace(String key) {
        return traces.get(key);
    }
}
