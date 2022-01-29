package org.infinity.luix.registry.consul;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.infinity.luix.registry.consul.ConsulService.TTL;

public class LuixConsulClientTest {

    private static LuixConsulClient    consulClient;
    private static ConsulHealthChecker consulHealthChecker;

    @BeforeAll
    public static void setup() {
        consulClient = new LuixConsulClient("localhost", 8500);
        consulHealthChecker = new ConsulHealthChecker(consulClient);
        consulHealthChecker.start();
    }

    @Test
    public void registerService() throws InterruptedException {
        ConsulService service1 = createConsulService("org.infinity.luix.democommon.service.MailService");
        ConsulService service2 = createConsulService("org.infinity.luix.democommon.service.AppService");
        consulClient.registerService(service1);
        consulClient.registerService(service2);
        consulHealthChecker.addCheckServiceId(service1.getInstanceName());
        consulHealthChecker.addCheckServiceId(service2.getInstanceName());
        consulHealthChecker.setHeartbeatOpen(true);
        Thread.sleep(100_000L);
    }

    @Test
    public void closeClient() {
        consulHealthChecker.close();
    }

    private static ConsulService createConsulService(String serviceName) {
        ConsulService service = new ConsulService();
        service.setName("luix");
        service.setInstanceName(serviceName + "@172.25.8.133:16010");
        service.setAddress("localhost");
        service.setPort(8500);
        service.setTags(Arrays.asList("protocol_luix"));
        return service;
    }
}
