package com.luixtech.luixrpc.core.config.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.config.Configurable;
import com.luixtech.luixrpc.core.exception.impl.RpcConfigException;
import com.luixtech.luixrpc.core.registry.Registry;
import com.luixtech.luixrpc.core.registry.factory.RegistryFactory;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.core.utils.RpcConfigValidator;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.luixtech.luixrpc.core.constant.ConsumerConstants.PROVIDER_ADDRESSES;
import static com.luixtech.luixrpc.core.constant.RegistryConstants.*;
import static com.luixtech.luixrpc.utilities.network.AddressUtils.LOCALHOST;

@Data
@Slf4j
public class RegistryConfig implements Configurable {
    public static final  String  PREFIX              = "registry";
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
    private              String  providerAddresses;
    /**
     * Timeout in milliseconds for connection session between client and server
     */
    private              Integer sessionTimeout;
    /**
     * Timeout in milliseconds when client creating a connection to server
     */
    @Positive
    private              Integer connectTimeout;
    /**
     * Registration and subscription retry interval in milliseconds
     * after the connection failure between provider, consumer and registry.
     */
    private              Integer retryInterval;
    /**
     * Service discovery interval in milliseconds
     */
    private              Integer discoveryInterval;
    /**
     * Registry url
     */
    private              Url     registryUrl;

    public void init() {
        checkIntegrity();
        checkValidity();
        registryUrl = createRegistryUrl();
        log.info("Luix registry configuration: {}", this);
    }

    @Override
    public void checkIntegrity() {
        if (name.equals(REGISTRY_VAL_ZOOKEEPER)) {
            RpcConfigValidator.notEmpty(host, "Please specify value of 'luix.registry.host' when 'luix.registry=zookeeper'!");
            RpcConfigValidator.notNull(port, "Please specify value of 'luix.registry.port' when 'luix.registry=zookeeper'!");
        } else if (name.equals(REGISTRY_VAL_NONE)) {
            RpcConfigValidator.notEmpty(providerAddresses, "Please specify value of 'luix.registry.providerAddresses' when 'luix.registry.name=none'!");
            RpcConfigValidator.mustEmpty(host, "Do NOT specify value of 'luix.registry.host' when 'luix.registry.name=none'!");
            RpcConfigValidator.mustNull(port, "Do NOT specify value of 'luix.registry.port' when 'luix.registry.name=none'!");
        }
    }

    @Override
    public void checkValidity() {
        Optional.ofNullable(RegistryFactory.getInstance(name))
                .orElseThrow(() -> new RpcConfigException("Failed to load the registry factory, " +
                        "please check whether the dependency [rpc-registry-" + name + "] is in your class path!"));


    }

    private Url createRegistryUrl() {
        Url registryUrl;
        if (name.equals(REGISTRY_VAL_NONE)) {
            // Build direct registry url
            registryUrl = Url.registryUrl(name, LOCALHOST, 0);
            registryUrl.addOption(PROVIDER_ADDRESSES, providerAddresses);
        } else {
            registryUrl = Url.registryUrl(name, host, port);
        }

        registryUrl.addOption(SESSION_TIMEOUT, sessionTimeout == null ? null : sessionTimeout.toString());
        registryUrl.addOption(CONNECT_TIMEOUT, connectTimeout == null ? null : connectTimeout.toString());
        registryUrl.addOption(RETRY_INTERVAL, retryInterval == null ? null : retryInterval.toString());
        registryUrl.addOption(DISCOVERY_INTERVAL, discoveryInterval == null ? null : discoveryInterval.toString());
        return registryUrl;
    }

    public Registry getRegistryImpl() {
        return RegistryFactory.getInstance(name).getRegistry(registryUrl);
    }
}