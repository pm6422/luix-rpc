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
import org.infinity.rpc.utilities.network.AddressUtils;
import org.infinity.rpc.utilities.serializer.Serializer;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.Optional;

import static org.infinity.rpc.core.constant.ProtocolConstants.*;

@Data
@Slf4j
public class ProtocolConfig implements Configurable {
    public static final String  PREFIX              = "protocol";
    /**
     * Name of protocol
     */
    @NotEmpty
    private             String  name                = PROTOCOL_VAL_INFINITY;
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
    @NotEmpty
    private             String  codec               = CODEC_VAL_DEFAULT;
    /**
     * Serializer used to encode request or deserializer used to decode response
     */
    @NotEmpty
    private             String  serializer          = SERIALIZER_VAL_DEFAULT;
    /**
     * Factory used to create client and server
     */
    @NotEmpty
    private             String  endpointFactory     = ENDPOINT_FACTORY_VAL_NETTY;
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
    @Positive
    private             int     maxServerConn       = MAX_SERVER_CONN_VAL_DEFAULT;
    /**
     * Allowed maximum response size in bytes
     */
    @Positive
    private             int     maxContentLength    = MAX_CONTENT_LENGTH_VAL_DEFAULT;
    /**
     * Minimum thread pool size on server side
     */
    @Positive
    private             int     minThread           = MIN_THREAD_VAL_DEFAULT;
    /**
     * Maximum thread pool size on server side
     */
    @Positive
    private             int     maxThread           = MAX_THREAD_VAL_DEFAULT;
    /**
     * Thread pool work queue size on server side
     */
    @PositiveOrZero
    private             int     workQueueSize       = WORK_QUEUE_SIZE_VAL_DEFAULT;
    /**
     * Indicator used to decide whether multiple servers share the same channel
     */
    private             boolean sharedChannel       = SHARED_CHANNEL_VAL_DEFAULT;
    /**
     * Indicator used to decide whether initialize client connection asynchronously
     */
    private             boolean asyncInitConn       = ASYNC_INIT_CONN_VAL_DEFAULT;
    /**
     * Indicator used to decide whether throw exception after request failure
     */
    private             boolean throwException      = THROW_EXCEPTION_VAL_DEFAULT;
    /**
     * Indicator used to decide whether transport response exception to client
     */
    private             boolean transExceptionStack = TRANS_EXCEPTION_STACK_VAL_DEFAULT;

    public void init() {
        checkIntegrity();
        checkValidity();
        initHost();
        log.info("Infinity RPC protocol configuration: {}", this);
    }

    @Override
    public void checkIntegrity() {
    }

    @Override
    public void checkValidity() {
        Optional.ofNullable(Protocol.getInstance(name))
                .orElseThrow(() -> new RpcConfigException(String.format("Failed to load the protocol [%s]!", name)));

        Optional.ofNullable(Codec.getInstance(codec))
                .orElseThrow(() -> new RpcConfigException(String.format("Failed to load the codec [%s]!", codec)));

        Optional.ofNullable(Serializer.getInstance(serializer))
                .orElseThrow(() -> new RpcConfigException(String.format("Failed to load the serializer [%s]!", serializer)));

        Optional.ofNullable(EndpointFactory.getInstance(endpointFactory))
                .orElseThrow(() -> new RpcConfigException(String.format("Failed to load the endpoint factory [%s]!", endpointFactory)));

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