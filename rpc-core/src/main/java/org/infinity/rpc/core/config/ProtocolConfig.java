package org.infinity.rpc.core.config;

import lombok.Data;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.utilities.network.NetworkUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Optional;

import static org.infinity.rpc.core.constant.ServiceConstants.PROTOCOL_DEFAULT_VALUE;

@Data
public class ProtocolConfig {
    /**
     * Name of protocol
     * SpringBoot properties binding mechanism can automatically convert the string value in config file to enum type,
     * and check whether value is valid or not during application startup.
     */
    @NotEmpty
    private String  name    = PROTOCOL_DEFAULT_VALUE;
    /**
     * Protocol version
     */
    @NotNull
    @Positive
    private Integer version = 1;
    /**
     * Host name of the RPC server
     * Generally, we do NOT need configure the value, it will be set automatically.
     * If there are exported providers, netty server will use it as starting host.
     * But if there are no exported providers, no netty server will be started.
     * todo: check intranet ip or other available host
     */
    @NotEmpty
    private String  host    = NetworkUtils.INTRANET_IP;
    /**
     * Port number of the RPC server
     * If there are exported providers, netty server will use it as starting port.
     */
    @NotNull
    @Positive
    private Integer port;

    public void init() {
        checkIntegrity();
        checkValidity();
    }

    private void checkIntegrity() {
    }

    private void checkValidity() {
        Optional.ofNullable(Protocol.getInstance(name))
                .orElseThrow(() -> new RpcConfigurationException("Failed to load the proper protocol instance, " +
                        "please check whether the correct dependency is in your class path!"));
    }
}