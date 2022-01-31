package org.infinity.luix.registry.consul;

import org.infinity.luix.core.url.Url;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.infinity.luix.core.constant.RegistryConstants.REGISTRY_VAL_CONSUL;

public class LuixConsulClientTest {

    private static ConsulRegistry consulRegistry;
    private static Url            provider1;

    @BeforeAll
    public static void setup() {
        Url registryUrl = Url.registryUrl(REGISTRY_VAL_CONSUL, "localhost", 8500);
        LuixConsulClient consulClient = new LuixConsulClient("localhost", 8500);
        consulRegistry = new ConsulRegistry(registryUrl, consulClient);
        provider1 = Url.valueOf("luix://10.10.10.118:16010/org.infinity.luix.democommon.service.AppAuthorityService?app=luix-demo-server&serializer=hessian2&type=provider");
    }

    @Test
    public void registerService() throws InterruptedException {
        consulRegistry.doRegister(provider1);
        consulRegistry.doActivate(provider1);
    }

    @Test
    public void deregisterService() {
        consulRegistry.doDeregister(provider1);
    }
}
