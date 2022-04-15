package com.luixtech.rpc.core.config.impl;

import com.luixtech.rpc.core.codec.Codec;
import com.luixtech.rpc.core.constant.ProtocolConstants;
import com.luixtech.rpc.core.exception.impl.RpcConfigException;
import com.luixtech.rpc.core.protocol.Protocol;
import com.luixtech.rpc.core.utils.RpcConfigValidator;
import com.luixtech.rpc.serializer.Serializer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.rpc.core.config.Configurable;
import com.luixtech.rpc.core.exchange.endpoint.NetworkTransmissionFactory;
import com.luixtech.rpc.core.utils.SerializerHolder;
import com.luixtech.utilities.network.AddressUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Optional;

@Data
@Slf4j
public class ProtocolConfig implements Configurable {
    public static final String  PREFIX = "protocol";
    /**
     * Name of protocol
     */
    @NotEmpty
    private             String  name   = ProtocolConstants.PROTOCOL_VAL_DEFAULT;
    /**
     * Host name of the RPC server
     * Generally, we do NOT need to configure the value, it will be set automatically.
     * If there are exposed providers, netty server will use it as starting host.
     * But if there are no exposed providers, no netty server will be started.
     */
    private             String  host;
    /**
     * Port number of the RPC server
     * If there are exposed providers, netty server will use it as starting port.
     */
    @NotNull
    @Positive
    private             Integer port;
    /**
     * Protocol codec used to encode request and decode response
     */
    private             String  codec;
    /**
     * Serializer used to encode request or deserializer used to decode response
     */
    private             String  serializer;
    /**
     * Factory used to create client and server
     */
    private             String  endpointFactory;
    /**
     * Minimum client channel count used to handle RPC request
     */
//    @Positive
    private             Integer minClientConn;
    /**
     * Allowed maximum client connecting failure count
     */
    private             Integer maxClientFailedConn;
    /**
     * Maximum server channel count used to handle RPC request
     */
    private             Integer maxServerConn;
    /**
     * Allowed maximum response size in bytes
     */
    private             Integer maxContentLength;
    /**
     * Minimum thread pool size on server side
     */
    private             Integer minThread;
    /**
     * Maximum thread pool size on server side
     */
    private             Integer maxThread;
    /**
     * Thread pool work queue size on server side
     */
    private             Integer workQueueSize;
    /**
     * Indicator used to decide whether multiple servers share the same channel
     */
    private             Boolean sharedChannel;
    /**
     * Indicator used to decide whether initialize client connection asynchronously
     */
    private             Boolean asyncInitConn;
    /**
     * Indicator used to decide whether throw exception after request failure
     */
    private             Boolean throwException;

    public void init() {
        checkIntegrity();
        checkValidity();
        initHost();
        SerializerHolder.init();
        log.info("Luix protocol configuration: {}", this);
    }

    @Override
    public void checkIntegrity() {
    }

    @Override
    public void checkValidity() {
        Optional.ofNullable(Protocol.getInstance(name))
                .orElseThrow(() -> new RpcConfigException(String.format("Failed to load the protocol [%s]!", name)));

        if (StringUtils.isNotEmpty(codec)) {
            Optional.ofNullable(Codec.getInstance(codec))
                    .orElseThrow(() -> new RpcConfigException(String.format("Failed to load the codec [%s]!", codec)));
        }

        if (StringUtils.isNotEmpty(serializer)) {
            Optional.ofNullable(Serializer.getInstance(serializer))
                    .orElseThrow(() -> new RpcConfigException(String.format("Failed to load the serializer [%s]!", serializer)));
        }

        if (StringUtils.isNotEmpty(endpointFactory)) {
            Optional.ofNullable(NetworkTransmissionFactory.getInstance(endpointFactory))
                    .orElseThrow(() -> new RpcConfigException(String.format("Failed to load the endpoint factory [%s]!", endpointFactory)));
        }

        if (StringUtils.isNotEmpty(host)) {
            RpcConfigValidator.isTrue(AddressUtils.isValidIp(host), "Please specify a valid host!");
        }
    }

    private void initHost() {
        if (StringUtils.isEmpty(host)) {
            host = AddressUtils.getIntranetIp();
        }
    }
}