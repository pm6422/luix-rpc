package org.infinity.rpc.core.config;

import lombok.Data;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcConfigValidator;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.PositiveOrZero;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.infinity.rpc.core.constant.ConsumerConstants.DIRECT_ADDRESSES;
import static org.infinity.rpc.core.constant.RegistryConstants.*;
import static org.infinity.rpc.utilities.network.AddressUtils.LOCALHOST;

@Data
public class RegistryConfig {
    private static final Pattern COLON_SPLIT_PATTERN = Pattern.compile("\\s*[:]+\\s*");
    /**
     * Name of register center
     */
    @NotEmpty
    private              String  name                = REGISTRY_VAL_ZOOKEEPER;
    /**
     * Registry center host name
     */
    private              String  host;
    /**
     * Registry center port number
     */
    @PositiveOrZero
    private              Integer port;
    /**
     * Addresses of RPC provider used to connect RPC provider directly without third party registry.
     * Multiple addresses are separated by comma.
     */
    private              String  directAddresses;
    /**
     * Timeout in milliseconds for connection session between registry client and server
     */
    @PositiveOrZero
    private              Integer sessionTimeout      = SESSION_TIMEOUT_VAL_DEFAULT;
    /**
     * Timeout in milliseconds when registry client building a connection to registry server
     */
    @PositiveOrZero
    private              Integer connectTimeout      = CONNECT_TIMEOUT_VAL_DEFAULT;
    /**
     * Registration and subscription retry interval in milliseconds
     * after the connection failure between provider, consumer and registry.
     */
    @PositiveOrZero
    private              Integer retryInterval       = RETRY_INTERVAL_VAL_DEFAULT;
    /**
     * Indicator used to decide whether need to throw exception after registering or unregistering failure
     */
    private              boolean throwException      = THROW_EXCEPTION_VAL_DEFAULT;
    /**
     * Registry url
     */
    private              Url     registryUrl;

    public void init() {
        checkIntegrity();
        checkValidity();
        registryUrl = createRegistryUrl();
    }

    private void checkIntegrity() {
        if (name.equals(REGISTRY_VAL_ZOOKEEPER)) {
            RpcConfigValidator.notEmpty(host, "Please specify value of 'infinity.registry.host' when 'infinity.registry=zookeeper'!");
            RpcConfigValidator.notNull(port, "Please specify value of 'infinity.registry.port' when 'infinity.registry=zookeeper'!");
        } else if (name.equals(REGISTRY_VAL_DIRECT)) {
            RpcConfigValidator.notEmpty(directAddresses, "Please specify value of 'infinity.registry.directAddresses' when 'infinity.registry=direct'!");
            RpcConfigValidator.mustEmpty(host, "Do NOT specify value of 'infinity.registry.host' when 'infinity.registry=direct'!");
            RpcConfigValidator.mustNull(port, "Do NOT specify value of 'infinity.registry.port' when 'infinity.registry=direct'!");
        }
    }

    private void checkValidity() {
        Optional.ofNullable(RegistryFactory.getInstance(name))
                .orElseThrow(() -> new RpcConfigurationException("Failed to load the specified registry factory, " +
                        "please check whether the dependency [rpc-registry-" + name + "] is in your class path!"));
    }

    private Url createRegistryUrl() {
        Url registryUrl;
        if (name.equals(REGISTRY_VAL_DIRECT)) {
            // Build direct registry url
            registryUrl = Url.registryUrl(name, LOCALHOST, 0);
            registryUrl.addOption(DIRECT_ADDRESSES, directAddresses);
        } else {
            registryUrl = Url.registryUrl(name, host, port);
        }

        registryUrl.addOption(SESSION_TIMEOUT, sessionTimeout.toString());
        registryUrl.addOption(CONNECT_TIMEOUT, connectTimeout.toString());
        registryUrl.addOption(RETRY_INTERVAL, retryInterval.toString());
        registryUrl.addOption(THROW_EXCEPTION, String.valueOf(throwException));
        return registryUrl;
    }

    public Registry getRegistryImpl() {
        return RegistryFactory.getInstance(name).getRegistry(registryUrl);
    }
}