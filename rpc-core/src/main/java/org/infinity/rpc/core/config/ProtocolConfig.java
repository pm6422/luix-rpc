package org.infinity.rpc.core.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.codec.Codec;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.network.LocalAddressFactory;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.utils.RpcConfigValidator;
import org.infinity.rpc.utilities.network.AddressUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Optional;

import static org.infinity.rpc.core.constant.ProtocolConstants.*;

@Data
public class ProtocolConfig {
    /**
     * Name of protocol
     */
    @NotEmpty
    private String  name                = PROTOCOL_DEFAULT_VALUE;
    /**
     * Protocol codec used to encode request and decode response
     * todo: codec和serializer的区别
     */
    @NotEmpty
    private String  codec               = CODEC_DEFAULT_VALUE;
    /**
     * Serializer used to encode request or deserializer used to decode response
     */
    @NotEmpty
    private String  serializer          = SERIALIZER_DEFAULT_VALUE;
    /**
     * Host name of the RPC server
     * Generally, we do NOT need configure the value, it will be set automatically.
     * If there are exported providers, netty server will use it as starting host.
     * But if there are no exported providers, no netty server will be started.
     */
    private String  host;
    /**
     * Port number of the RPC server
     * If there are exported providers, netty server will use it as starting port.
     */
    @NotNull
    @Positive
    private Integer port;
    /**
     * Factory used to get the local address
     */
    @NotEmpty
    private String  localAddressFactory = LOCAL_ADDRESS_FACTORY_DEFAULT_VALUE;
    /**
     * Factory used to create client and server
     */
    @NotEmpty
    private String  endpointFactory     = ENDPOINT_FACTORY_DEFAULT_VALUE;
    /**
     * Minimum client channel count used to handle RPC request
     */
    @Positive
    private int     minClientConn       = MIN_CLIENT_CONN_DEFAULT_VALUE;
    /**
     * Allowed maximum client connecting failure count
     */
    @Positive
    private int     maxClientFailedConn = MAX_CLIENT_FAILED_CONN_DEFAULT_VALUE;
    /**
     * Maximum server channel count used to handle RPC request
     */
    @Positive
    private int     maxServerConn       = MAX_SERVER_CONN_DEFAULT_VALUE;
    /**
     * Allowed maximum response size in bytes
     */
    @Positive
    private int     maxContentLength    = MAX_CONTENT_LENGTH_DEFAULT_VALUE;
    /**
     * Minimum thread pool size on server side
     */
    @Positive
    private int     minThread           = MIN_THREAD_DEFAULT_VALUE;
    /**
     * Maximum thread pool size on server side
     */
    @Positive
    private int     maxThread           = MAX_THREAD_DEFAULT_VALUE;
    /**
     * Thread pool work queue size on server side
     */
    @Positive
    private int     workQueueSize       = WORK_QUEUE_SIZE_DEFAULT_VALUE;
    /**
     * Indicator used to decide whether multiple servers share the same channel
     */
    private boolean sharedChannel       = SHARED_CHANNEL_DEFAULT_VALUE;
    /**
     * Indicator used to decide whether initialize client connection asynchronously
     */
    private boolean asyncInitConn       = ASYNC_INIT_CONN_DEFAULT_VALUE;
    /**
     * Indicator used to decide whether throw exception after request failure
     */
    private boolean throwException      = THROW_EXCEPTION_DEFAULT_VALUE;
    /**
     * Indicator used to decide whether transport response exception to client
     */
    private boolean transExceptionStack = TRANS_EXCEPTION_STACK_DEFAULT_VALUE;

    public void init() {
        checkIntegrity();
        checkValidity();
        initHost();
    }

    private void checkIntegrity() {
    }

    private void checkValidity() {
        Optional.ofNullable(Protocol.getInstance(name))
                .orElseThrow(() -> new RpcConfigurationException(String.format("Failed to load the specified protocol [%s]!", name)));

        Optional.ofNullable(Codec.getInstance(codec))
                .orElseThrow(() -> new RpcConfigurationException(String.format("Failed to load the specified codec [%s]!", codec)));

        if (StringUtils.isNotEmpty(host)) {
            RpcConfigValidator.isTrue(AddressUtils.isValidAddress(host), "Please specify a valid host!");
        }
    }

    private void initHost() {
        if (StringUtils.isEmpty(host)) {
            host = LocalAddressFactory.getInstance(localAddressFactory).getLocalAddress();
        }
    }
}