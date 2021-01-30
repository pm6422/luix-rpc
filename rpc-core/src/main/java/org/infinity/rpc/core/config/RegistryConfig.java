package org.infinity.rpc.core.config;

import lombok.Data;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.url.Url;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
    @NotEmpty
    private              String  host                = "localhost";
    /**
     * Registry center port number
     */
    @NotNull
    @Positive
    private              Integer port;
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
    }

    private Url createRegistryUrl() {
        Url registryUrl = Url.registryUrl(name, host, port);

        // Assign values to parameters
        registryUrl.addOption(CHECK_HEALTH, String.valueOf(CHECK_HEALTH_DEFAULT_VALUE));
        registryUrl.addOption(ADDRESS, registryUrl.getAddress());
        registryUrl.addOption(CONNECT_TIMEOUT, connectTimeout.toString());
        registryUrl.addOption(SESSION_TIMEOUT, sessionTimeout.toString());
        registryUrl.addOption(RETRY_INTERVAL, retryInterval.toString());
        return registryUrl;
    }

    public Registry getRegistryImpl() {
        return RegistryFactory.getInstance(name).getRegistry(registryUrl);
    }

}