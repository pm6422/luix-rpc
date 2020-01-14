package org.infinity.springboot.infinityrpc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.infinity-rpc")
@Data
public class RpcClientProperties {
    public static final String   CONSUMER_SCAN_PACKAGES = "spring.infinity-rpc.consumer.scan-packages";
    private             Consumer consumer;
    private             Registry registry;

    public static class Consumer {
        private String[] scanPackages;

        public String[] getScanPackages() {
            return scanPackages;
        }

        public void setScanPackages(String[] scanPackages) {
            this.scanPackages = scanPackages;
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
