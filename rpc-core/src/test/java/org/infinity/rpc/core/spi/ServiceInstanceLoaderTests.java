package org.infinity.rpc.core.spi;

import junit.framework.TestCase;
import org.infinity.rpc.core.spi.testservice.SpiPrototypeInterface;
import org.infinity.rpc.core.spi.testservice.SpiSingletonInterface;
import org.junit.Assert;
import org.junit.Test;

public class ServiceInstanceLoaderTests extends TestCase {

    @Test
    public void testSingletonInitialization() {
        // 单例模式下只会构造一次实例
        Assert.assertEquals(1, ServiceInstanceLoader.getServiceLoader(SpiSingletonInterface.class).getServiceImpl("spitest").spiHello());
        Assert.assertEquals(1, ServiceInstanceLoader.getServiceLoader(SpiSingletonInterface.class).getServiceImpl("spitest").spiHello());
    }

    @Test
    public void testPrototypeInitialization() {
        // 多例模式下在每次获取的时候进行实例化
        Assert.assertEquals(1, ServiceInstanceLoader.getServiceLoader(SpiPrototypeInterface.class).getServiceImpl("spiPrototypeTest").spiHello());
        Assert.assertEquals(2, ServiceInstanceLoader.getServiceLoader(SpiPrototypeInterface.class).getServiceImpl("spiPrototypeTest").spiHello());
    }
}
