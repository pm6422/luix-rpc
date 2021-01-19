package org.infinity.rpc.spring.boot.config;

import lombok.Data;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.infinity.rpc.core.constant.ServiceConstants.REGISTRY_DEFAULT_VALUE;

@Data
@Validated
public class RegistryConfig {
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

    public void init() {
        checkIntegrity();
        checkValidity();
    }

    private void checkIntegrity() {
    }

    private void checkValidity() {
        Optional.ofNullable(RegistryFactory.getInstance(name))
                .orElseThrow(() -> new RpcConfigurationException("Failed to load the correct registry factory, " +
                        "please check whether the dependency [rpc-registry-" + name + "] is in your class path!"));
    }
}