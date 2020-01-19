package org.infinity.springboot.infinityrpc;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@ConfigurationProperties(prefix = "spring.infinity-rpc")
@Data
public class InfinityRpcProperties implements InitializingBean {
    public static final String      SCAN_PACKAGES       = "spring.infinity-rpc.package-scan.scan-packages";
    public static final Pattern     COLON_SPLIT_PATTERN = Pattern.compile("\\s*[:]+\\s*");
    private             PackageScan packageScan;
    private             Registry    registry;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!StringUtils.isEmpty(registry.getAddress()) && StringUtils.isEmpty(registry.getServer()) && registry.getPort() == null) {
            String[] splitParts = COLON_SPLIT_PATTERN.split(registry.getAddress());
            registry.setServer(splitParts[0]);
            registry.setPort(Integer.parseInt(splitParts[1]));
        }
    }

    @Data
    public static class PackageScan {
        // The base packages to scan, if multiple values must be delimited by comma
        private String[] scanPackages;
    }

    @Data
    public static class Registry {
        // Registry center server address
        private String  address;
        // Registry center server name
        private String  server;
        // Registry center port number
        private Integer port;
    }
}
