package org.infinity.rpc.core.config.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.codec.Codec;
import org.infinity.rpc.core.config.Configurable;
import org.infinity.rpc.core.exception.impl.RpcConfigException;
import org.infinity.rpc.core.exchange.endpoint.EndpointFactory;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.utils.RpcConfigValidator;
import org.infinity.rpc.core.utils.SerializerHolder;
import org.infinity.rpc.utilities.network.AddressUtils;
import org.infinity.rpc.utilities.serializer.Serializer;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Optional;

import static org.infinity.rpc.core.constant.ProtocolConstants.PROTOCOL_VAL_INFINITY;

@Data
@Slf4j
public class ProtocolConfig implements Configurable {
    public static final String  PREFIX = "protocol";
    /**
     * Name of protocol
     */
    @NotEmpty
    private             String  name   = PROTOCOL_VAL_INFINITY;
    /**
     * Host name of the RPC server
     * Generally, we do NOT need configure the value, it will be set automatically.
     * If there are exported providers, netty server will use it as starting host.
     * But if there are no exported providers, no netty server will be started.
     */
    private             String  host;
    /**
     * Port number of the RPC server
     * If there are exported providers, netty server will use it as starting port.
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
        log.info("Infinity RPC protocol configuration: {}", this);
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
            Optional.ofNullable(EndpointFactory.getInstance(endpointFactory))
                    .orElseThrow(() -> new RpcConfigException(String.format("Failed to load the endpoint factory [%s]!", endpointFactory)));
        }

        if (StringUtils.isNotEmpty(host)) {
            RpcConfigValidator.isTrue(AddressUtils.isValidIp(host), "Please specify a valid host!");
        }
    }

    private void initHost() {
        if (StringUtils.isEmpty(host)) {
            host = AddressUtils.getLocalIp();
        }
    }
}