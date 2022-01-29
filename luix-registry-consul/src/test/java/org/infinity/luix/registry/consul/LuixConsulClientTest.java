package org.infinity.luix.registry.consul;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.infinity.luix.registry.consul.ConsulService.TTL;

public class LuixConsulClientTest {

    @Test
    public void registerService() throws InterruptedException {
        LuixConsulClient consulClient = new LuixConsulClient("localhost", 8500);
        ConsulHealthChecker consulHealthChecker = new ConsulHealthChecker(consulClient);
        consulHealthChecker.start();
        ConsulService service = createConsulService();
        consulClient.registerService(service);

        consulHealthChecker.addCheckServiceId(service.getId());
        consulHealthChecker.setHeartbeatOpen(true);
        Thread.sleep(100_000L);
    }

    private static ConsulService createConsulService() {
        ConsulService service = new ConsulService();
        service.setId("172.25.8.133:16010-org.infinity.luix.democommon.service.AppService");
        service.setName("luix");
        service.setAddress("localhost");
        service.setPort(8500);
        service.setTtl(TTL);
        service.setTags(Arrays.asList("protocol_luix", "URL_luix%3A%2F%2F172.25.8.133%3A16010%2Forg.infinity.luix.democommon.service.AppService%3Fapp%3Dluix-demo-server%26form%3Df2%26retryCount%3D1%26serializer%3Dhessian2%26type%3Dprovider"));
        return service;
    }
}
