package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.constants.RegistryName;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Data
@Validated
public class RegistryConfig {
    private static final Pattern      COLON_SPLIT_PATTERN = Pattern.compile("\\s*[:]+\\s*");
    /**
     * Name of register center
     */
    @NotNull
    private              RegistryName name                = RegistryName.zookeeper;
    /**
     * Registry center host name
     */
    @NotEmpty
    private              String       host                = "localhost";
    /**
     * Registry center port number
     */
    @NotNull
    @Positive
    private              Integer      port;
    /**
     * 注册中心连接超时时间(毫秒)
     */
    @PositiveOrZero
    private              Integer      connectTimeout      = Math.toIntExact(TimeUnit.SECONDS.toMillis(1));
    /**
     * 注册中心会话超时时间(毫秒)
     */
    @PositiveOrZero
    private              Integer      sessionTimeout      = Math.toIntExact(TimeUnit.MINUTES.toMillis(1));
    /**
     * 注册中心连接失败后重试的时间间隔(毫秒)
     */
    @PositiveOrZero
    private              Integer      retryInterval       = Math.toIntExact(TimeUnit.SECONDS.toMillis(30));
    /**
     * 注册中心请求超时时间(毫秒)
     */
    @PositiveOrZero
    private              Integer      requestTimeout;

    public void init() {
        checkIntegrity();
        checkValidity();
    }

    private void checkIntegrity() {
    }

    private void checkValidity() {
        Optional.ofNullable(RegistryFactory.getInstance(name.getValue()))
                .orElseThrow(() -> new RpcConfigurationException("Failed to load the correct registry factory, " +
                        "please check whether the dependency [rpc-registry-" + name.getValue() + "] is in your class path!"));
    }
}