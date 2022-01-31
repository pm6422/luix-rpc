package org.infinity.luix.registry.consul;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class LuixConsulClientTest {

    private static LuixConsulClient    consulClient;
    private static ConsulStatusUpdater consulStatusUpdater;

    @BeforeAll
    public static void setup() {
        consulClient = new LuixConsulClient("localhost", 8500);
        consulStatusUpdater = new ConsulStatusUpdater(consulClient);
        consulStatusUpdater.start();
    }

    @Test
    public void registerService() throws InterruptedException {
        ConsulService service1 = createConsulService("org.infinity.luix.democommon.service.MailService", "127.0.0.1", 6010);
        ConsulService service2 = createConsulService("org.infinity.luix.democommon.service.AppService", "127.0.0.1", 6020);
        consulClient.registerService(service1);
        consulClient.registerService(service2);
//        consulHealthChecker.addCheckingServiceInstanceId(service1.getInstanceId());
//        consulHealthChecker.addCheckingServiceInstanceId(service2.getInstanceId());
//        consulHealthChecker.setCheckHealthSwitcherStatus(true);

        consulClient.deactivate(service1.getInstanceId());
        consulClient.deactivate(service2.getInstanceId());
        Thread.sleep(100_000L);
    }

    @Test
    public void closeClient() {
        consulStatusUpdater.close();
    }

    private static ConsulService createConsulService(String serviceName, String host, int port) {
        ConsulService service = new ConsulService();
        service.setName("luix-providing");
        service.setInstanceId(serviceName + "@" + host + ":" + port);
        service.setAddress(host);
        service.setPort(port);
        service.setTags(Arrays.asList("protocol_luix"));
        return service;
    }
}
