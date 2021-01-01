package org.infinity.rpc.utilities.spi;

import junit.framework.TestCase;
import org.infinity.rpc.utilities.spi.testservice.SpiPrototypeInterface;
import org.infinity.rpc.utilities.spi.testservice.impl.SpiSingletonInterface;
import org.junit.Assert;
import org.junit.Test;

public class ServiceLoaderTests extends TestCase {

    @Test
    public void testSingletonInitialization() {
        // 单例模式下只会构造一次实例
        Assert.assertEquals(1, ServiceLoader.forClass(SpiSingletonInterface.class)
                .load("spitest").spiHello());
        Assert.assertEquals(1, ServiceLoader.forClass(SpiSingletonInterface.class)
                .load("spitest").spiHello());
    }

    @Test
    public void testPrototypeInitialization() {
        // 多例模式下在每次获取的时候进行实例化
        Assert.assertEquals(1, ServiceLoader.forClass(SpiPrototypeInterface.class)
                .load("spiPrototypeTest").spiHello());
        Assert.assertEquals(2, ServiceLoader.forClass(SpiPrototypeInterface.class)
                .load("spiPrototypeTest").spiHello());
    }

    @Test
    public void testChineseCharacterLoad() {
        // 单例模式下只会构造一次实例
        Assert.assertEquals(1, ServiceLoader.forClass(SpiSingletonInterface.class)
                .load("单例").spiHello());
    }
}
