package org.infinity.luix.utilities.serviceloader;

import junit.framework.TestCase;
import org.infinity.luix.utilities.serviceloader.testservice.SpiPrototypeInterface;
import org.infinity.luix.utilities.serviceloader.testservice.SpiSingletonInterface;
import org.junit.Assert;
import org.junit.Test;

public class ServiceLoaderTests extends TestCase {

    @Test
    public void testSingletonInitialization() {
        // 单例模式下只会构造一次实例
        Assert.assertEquals(1, ServiceLoader.forClass(SpiSingletonInterface.class)
                .load("singleton").spiHello());
        Assert.assertEquals(1, ServiceLoader.forClass(SpiSingletonInterface.class)
                .load("singleton").spiHello());
    }

    @Test
    public void testPrototypeInitialization() {
        // 多例模式下在每次获取的时候进行实例化
        Assert.assertEquals(1, ServiceLoader.forClass(SpiPrototypeInterface.class)
                .load("prototype").spiHello());
        Assert.assertEquals(2, ServiceLoader.forClass(SpiPrototypeInterface.class)
                .load("prototype").spiHello());
    }

    @Test
    public void testChineseCharacterLoad() {
        // 单例模式下只会构造一次实例
        Assert.assertEquals(1, ServiceLoader.forClass(SpiSingletonInterface.class)
                .load("单例").spiHello());
    }
}
