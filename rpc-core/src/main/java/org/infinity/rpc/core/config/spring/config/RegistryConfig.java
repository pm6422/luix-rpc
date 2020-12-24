package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.constants.RegistryName;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Data
@Validated
public class RegistryConfig {
    private static final Pattern      COLON_SPLIT_PATTERN = Pattern.compile("\\s*[:]+\\s*");
    // Name of register center
    private              RegistryName name                = RegistryName.zookeeper;
    // Registry center host name
    private              String       host;
    // Registry center port number
    private              Integer      port;
    // Registry center server address
    private              String       address;
    // 注册中心连接超时时间(毫秒)
    private              Integer      connectTimeout      = Math.toIntExact(TimeUnit.SECONDS.toMillis(1));
    // 注册中心会话超时时间(毫秒)
    private              Integer      sessionTimeout      = Math.toIntExact(TimeUnit.MINUTES.toMillis(1));
    // 注册中心连接失败后重试的时间间隔(毫秒)
    private              Integer      retryInterval       = Math.toIntExact(TimeUnit.SECONDS.toMillis(30));
    // 注册中心请求超时时间(毫秒)
    private              Integer      requestTimeout;

    public void initialize() {
        checkIntegrity();
        checkValidity();
        if (StringUtils.isNotEmpty(address) && StringUtils.isEmpty(host) && port == null) {
            String[] splitParts = COLON_SPLIT_PATTERN.split(address);
            host = splitParts[0];
            port = Integer.parseInt(splitParts[1]);
        }
        if (StringUtils.isEmpty(address) && StringUtils.isNotEmpty(host) && port != null) {
            address = host + ":" + port;
        }
    }

    private void checkIntegrity() {
        // todo
    }

    private void checkValidity() {
        Optional.ofNullable(RegistryFactory.getInstance(name.getValue()))
                .orElseThrow(() -> new RpcConfigurationException("Failed to load the correct registry factory, " +
                        "please check whether the dependency [rpc-registry-" + name.getValue() + "] is in your class path!"));
    }
}