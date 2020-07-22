package org.infinity.rpc.utilities.id;

import org.infinity.rpc.utilities.id.sequence.SnowFlakeSequence;
import org.junit.Test;

public class SnowFlakeSequenceTest1 {

    @Test
    public void name() {
        try {
            int times = 0, maxTimes = 1000;
            SnowFlakeSequence snowFlakeSequence = new SnowFlakeSequence(0);
            for (int i = 0; i < maxTimes; i++) {
                long id = snowFlakeSequence.nextId();
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
