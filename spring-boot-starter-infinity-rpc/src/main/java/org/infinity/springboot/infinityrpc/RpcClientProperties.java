package org.infinity.springboot.infinityrpc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.infinity-rpc")
@Data
public class RpcClientProperties {
    public static final String   CONSUMER_SCAN_BASE_PACKAGES = "spring.infinity-rpc.client.base-packages";
    private             Client   client;
    private             Registry registry;

    public static class Client {
        private String[] basePackages;

        public String[] getBasePackages() {
            return basePackages;
        }

        public void setBasePackages(String[] basePackages) {
            this.basePackages = basePackages;
        }
    }

    public static class Registry {
        private String address;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }
}
