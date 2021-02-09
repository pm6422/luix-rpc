package org.infinity.rpc.core.config;

import lombok.Data;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcConfigValidator;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.infinity.rpc.core.constant.ConsumerConstants.DIRECT_ADDRESSES;
import static org.infinity.rpc.core.constant.ServiceConstants.*;

@Data
public class RegistryConfig {
    public static final String CONNECT_TIMEOUT               = "connectTimeout";
    public static final int    CONNECT_TIMEOUT_DEFAULT_VALUE = 1000;
    public static final String SESSION_TIMEOUT               = "sessionTimeout";
    public static final int    SESSION_TIMEOUT_DEFAULT_VALUE = 1000;
    public static final String RETRY_INTERVAL                = "retryInterval";
    public static final String ADDRESS                       = "address";

    private static final Pattern COLON_SPLIT_PATTERN = Pattern.compile("\\s*[:]+\\s*");
    /**
     * Name of register center
     */
    @NotEmpty
    private              String  name                = REGISTRY_DEFAULT_VALUE;
    /**
     * Registry center host name
     */
    private              String  host                ;
    /**
     * Registry center port number
     */
    @PositiveOrZero
    private              Integer port                ;
    /**
     * Addresses of RPC provider used to connect RPC provider directly without third party registry.
     * Multiple addresses are separated by comma.
     */
    private              String  directAddresses;
    /**
     * 注册中心连接超时时间(毫秒)
     */
    @PositiveOrZero
    private              Integer connectTimeout      = Math.toIntExact(TimeUnit.SECONDS.toMillis(1));
    /**
     * 注册中心会话超时时间(毫秒)
     */
    @PositiveOrZero
    private              Integer sessionTimeout      = Math.toIntExact(TimeUnit.MINUTES.toMillis(1));
    /**
     * 注册中心连接失败后重试的时间间隔(毫秒)
     */
    @PositiveOrZero
    private              Integer retryInterval       = Math.toIntExact(TimeUnit.SECONDS.toMillis(30));
    /**
     * 注册中心请求超时时间(毫秒)
     */
    @PositiveOrZero
    private              Integer requestTimeout;
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
    }

    private void checkValidity() {
        Optional.ofNullable(RegistryFactory.getInstance(name))
                .orElseThrow(() -> new RpcConfigurationException("Failed to load the correct registry factory, " +
                        "please check whether the dependency [rpc-registry-" + name + "] is in your class path!"));

        if (name.equals(REGISTRY_DEFAULT_VALUE)) {
            RpcConfigValidator.notEmpty(host, "Please specify value of 'infinity.registry.host' when 'infinity.registry=zookeeper'!");
            RpcConfigValidator.notNull(port, "Please specify value of 'infinity.registry.port' when 'infinity.registry=zookeeper'!");
        } else if (name.equals(REGISTRY_VALUE_DIRECT)) {
            RpcConfigValidator.notEmpty(directAddresses, "Please specify value of 'infinity.registry.directUrls' when 'infinity.registry=direct'!");
            RpcConfigValidator.mustEmpty(host, "Do NOT specify value of 'infinity.registry.host' when 'infinity.registry=direct'!");
            RpcConfigValidator.mustNull(port, "Do NOT specify value of 'infinity.registry.port' when 'infinity.registry=direct'!");
        }
    }

    private Url createRegistryUrl() {
        Url registryUrl;
        if (name.equals(REGISTRY_VALUE_DIRECT)) {
            // if it is direct
            registryUrl = Url.registryUrl(name, "127.0.0.1", 0);
            registryUrl.addOption(DIRECT_ADDRESSES, directAddresses);
        } else {
            registryUrl = Url.registryUrl(name, host, port);
        }

        // Assign values to parameters
        registryUrl.addOption(CHECK_HEALTH, String.valueOf(CHECK_HEALTH_DEFAULT_VALUE));
        registryUrl.addOption(CONNECT_TIMEOUT, connectTimeout.toString());
        registryUrl.addOption(SESSION_TIMEOUT, sessionTimeout.toString());
        registryUrl.addOption(RETRY_INTERVAL, retryInterval.toString());
        return registryUrl;
    }

    public Registry getRegistryImpl() {
        return RegistryFactory.getInstance(name).getRegistry(registryUrl);
    }
}