package com.luixtech.rpc.core.thread;

import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorServiceTests {
    @Test
    public void testSchedule() throws InterruptedException {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.schedule(this::print1, 1, TimeUnit.MILLISECONDS);
        scheduledExecutorService.schedule(this::print2, 1, TimeUnit.MILLISECONDS);
        Thread.sleep(4);
    }

    private void print1() {
        System.out.println("print1");
    }

    private void print2() {
        System.out.println("print2");
    }
}
