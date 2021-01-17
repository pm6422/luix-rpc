package org.infinity.rpc.core.exchange.request.impl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.protocol.constants.ProtocolVersion;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@ToString
public class RpcRequest implements Requestable, Serializable {
    private static final long                serialVersionUID = -6259178379027752471L;
    private              long                requestId;
    private              byte                protocolVersion  = ProtocolVersion.VERSION_1.getVersion();
    private              String              interfaceName;
    private              String              methodName;
    /**
     * The method parameter type name list string which is separated by comma.
     * e.g, java.util.List,java.lang.Long
     */
    private              String              methodParameters;
    private              Object[]            methodArguments;
    private              int                 retryNumber;
    private              long                sendingTime;
    private              long                receivedTime;
    private              long                elapsedTime;
    private              Map<String, String> traces           = new ConcurrentHashMap<>();
    /**
     * RPC request options, all the optional RPC request parameters will be put in it.
     */
    private              Map<String, String> options          = new ConcurrentHashMap<>();
    /**
     * Default serialization is Hession2
     */
    private              int                 serializeNum     = 0;

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
