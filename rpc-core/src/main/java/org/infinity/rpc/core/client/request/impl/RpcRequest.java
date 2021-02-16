package org.infinity.rpc.core.client.request.impl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.protocol.constants.ProtocolVersion;

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
    protected            byte                protocolVersion  = ProtocolVersion.VERSION_1.getVersion();
    protected            String              interfaceName;
    protected            String              methodName;
    /**
     * The method parameter type name list string which is separated by comma.
     * e.g, java.util.List,java.lang.Long
     */
    protected            String              methodParameters;
    protected            Object[]            methodArguments;
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
     * Default serialization is Hession2
     */
    protected            int                 serializeNum     = 0;

    public RpcRequest(long requestId, String interfaceName, String methodName, String methodParameters) {
        this.requestId = requestId;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.methodParameters = methodParameters;
    }

    @Override
    public void addOption(String key, String value) {
        options.putIfAbsent(key, value);
    }

    @Override
    public String getOption(String key) {
        return options.get(key);
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
