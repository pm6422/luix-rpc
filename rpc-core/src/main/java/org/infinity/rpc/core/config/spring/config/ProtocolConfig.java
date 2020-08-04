package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.protocol.constants.ProtocolName;
import org.infinity.rpc.utilities.network.NetworkIpUtils;

import java.util.Optional;

@Data
public class ProtocolConfig {
    // Name of protocol
    // SpringBoot properties binding mechanism can automatically convert the string value in config file to enum type,
    // and check whether value is valid or not during application startup.
    private ProtocolName name             = ProtocolName.infinity;
    // Host name of the RPC server
    private String       host             = NetworkIpUtils.INTRANET_IP;
    // Port number of the RPC server
    private Integer      port;
    // Cluster implementation
    private String       cluster          = "default";
    // Cluster loadBalancer implementation
    private String       loadBalancer     = "random";
    // High availability strategy
    private String       highAvailability = "failover";

    public void initialize() {
        checkIntegrity();
        checkValidity();
    }

    private void checkIntegrity() {
        Validate.notNull(port, "Protocol port must NOT be null! Please check your configuration.");
    }

    private void checkValidity() {
        Optional.ofNullable(Protocol.getInstance(name.getValue()))
                .orElseThrow(() -> new RpcConfigurationException("Failed to load the proper protocol instance, " +
                        "please check whether the correct dependency is in your class path!"));
    }
}