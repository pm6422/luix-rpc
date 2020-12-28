package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.protocol.constants.ProtocolName;
import org.infinity.rpc.utilities.network.NetworkUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Optional;

@Data
@Validated
public class ProtocolConfig {
    /**
     * Name of protocol
     * SpringBoot properties binding mechanism can automatically convert the string value in config file to enum type,
     * and check whether value is valid or not during application startup.
     */
    @NotNull
    private ProtocolName name           = ProtocolName.infinity;
    /**
     * Host name of the RPC server
     */
    @NotEmpty
    private String       host           = NetworkUtils.INTRANET_IP;
    /**
     * Port number of the RPC server
     */
    @NotNull
    @Positive
    private Integer      port;
    /**
     * Cluster implementation
     */
    @NotEmpty
    private String       cluster        = "default";
    /**
     * Cluster loadBalancer implementation
     */
    @NotEmpty
    private String       loadBalancer   = "random";
    /**
     * Fault tolerance strategy
     */
    @NotEmpty
    private String       faultTolerance = "failover";

    public void init() {
        checkIntegrity();
        checkValidity();
        // Initialize provider cluster before consumer initialization
        createProviderCluster();
    }

    private void checkIntegrity() {
    }

    private void checkValidity() {
        Optional.ofNullable(Protocol.getInstance(name.getValue()))
                .orElseThrow(() -> new RpcConfigurationException("Failed to load the proper protocol instance, " +
                        "please check whether the correct dependency is in your class path!"));
    }

    private void createProviderCluster() {
        // todo: support multiple protocols
        // 当配置多个protocol的时候，比如A,B,C，
        // 那么正常情况下只会使用A，如果A被开关降级，那么就会使用B，B也被降级，那么会使用C
        // One cluster is created for one protocol, only one server node under a cluster can receive the request
        ProviderCluster.createCluster(name.name(), cluster, loadBalancer, faultTolerance);
    }
}