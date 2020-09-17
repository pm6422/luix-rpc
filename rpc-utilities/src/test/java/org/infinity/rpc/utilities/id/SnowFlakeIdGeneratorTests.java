package org.infinity.rpc.utilities.id;

import org.junit.Test;

public class SnowFlakeIdGeneratorTests {

    @Test
    public void testArguments() throws Exception {
        System.out.println("场景一：毫秒内固定起始值开始");
        testSequence();
        System.out.println("场景二：毫秒内随机起始值开始");
        testRandomSequence();
    }

    /**
     * 场景一：毫秒内固定起始值开始
     */
    private static void testSequence() throws Exception {
        SnowFlakeIdGenerator snowFlakeIdGenerator = new SnowFlakeIdGenerator(1L, false, false);
        for (int i = 0; i < 100; i++) {
            Thread.sleep(1);
            System.out.println(snowFlakeIdGenerator.nextId());
        }
    }

    /**
     * 场景二：毫秒内随机起始值开始
     */
    private static void testRandomSequence() throws Exception {
        SnowFlakeIdGenerator snowFlakeIdGenerator = new SnowFlakeIdGenerator(1L, false, true);
        for (int i = 0; i < 100; i++) {
            Thread.sleep(1);
            System.out.println(snowFlakeIdGenerator.nextId());
        }
    }

    @Test
    public void testEvenOddValue() {
        try {
            int times = 0, maxTimes = 1000;
            SnowFlakeIdGenerator snowFlakeIdGenerator = new SnowFlakeIdGenerator(0);
            for (int i = 0; i < maxTimes; i++) {
                long id = snowFlakeIdGenerator.nextId();
                if (id % 2 == 0) {
                    times++;
                }
                Thread.sleep(10);
            }
            System.out.println("偶数:" + times + ",奇数:" + (maxTimes - times) + "!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
