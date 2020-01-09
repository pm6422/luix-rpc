package org.infinity.rpc.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.rpc")
@Data
public class RpcClientProperties {
    private Client   client;
    private Registry registry;

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
