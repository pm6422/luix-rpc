package org.infinity.rpc.spring.boot.config;

import lombok.Data;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.utilities.network.NetworkUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Optional;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.core.constant.ServiceConstants.PROTOCOL_DEFAULT_VALUE;

@Data
@Validated
public class ProtocolConfig {
    /**
     * Name of protocol
     * SpringBoot properties binding mechanism can automatically convert the string value in config file to enum type,
     * and check whether value is valid or not during application startup.
     */
    @NotNull
    private String  name               = PROTOCOL_DEFAULT_VALUE;
    /**
     * Protocol version
     */
    @NotNull
    @Positive
    private Integer version            = 1;
    /**
     * Host name of the RPC server
     */
    @NotEmpty
    private String  host               = NetworkUtils.INTRANET_IP;
    /**
     * Port number of the RPC server
     */
    @NotNull
    @Positive
    private Integer port;
    /**
     * Cluster implementation
     */
    @NotEmpty
    private String  cluster            = CLUSTER_DEFAULT_VALUE;
    /**
     * Cluster loadBalancer implementation
     */
    @NotEmpty
    private String  loadBalancer       = LOAD_BALANCER_DEFAULT_VALUE;
    /**
     * Fault tolerance strategy
     */
    @NotEmpty
    private String  faultTolerance     = FAULT_TOLERANCE_DEFAULT_VALUE;
    /**
     * Check health factory
     */
    @NotEmpty
    private String  checkHealthFactory = CHECK_HEALTH_FACTORY_DEFAULT_VALUE;

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