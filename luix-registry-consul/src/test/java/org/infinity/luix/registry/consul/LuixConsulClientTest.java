package org.infinity.luix.registry.consul;

import org.infinity.luix.core.url.Url;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.infinity.luix.core.constant.ProtocolConstants.PROTOCOL_VAL_LUIX;

public class LuixConsulClientTest {

    private static ConsulRegistry consulRegistry;
    private static Url            provider1;

    @BeforeAll
    public static void setup() {
        Url registryUrl = Url.registryUrl(PROTOCOL_VAL_LUIX, "localhost", 8500);
        LuixConsulClient consulClient = new LuixConsulClient("localhost", 8500);
        consulRegistry = new ConsulRegistry(registryUrl, consulClient);
        provider1 = Url.providerUrl(PROTOCOL_VAL_LUIX, "127.0.0.1", 6010, "org.infinity.luix.democommon.service.MailService");
    }

    @Test
    public void registerService() throws InterruptedException {
        consulRegistry.doRegister(provider1);
        consulRegistry.doActivate(provider1);
        Thread.sleep(100_000L);
    }

    @Test
    public void deregisterService() {
        consulRegistry.doDeregister(provider1);
    }
}
