package org.infinity.luix.registry.consul;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.utilities.collection.ConcurrentHashSet;

import java.util.concurrent.*;

import static org.infinity.luix.registry.consul.ConsulService.TTL;

/**
 * consul 心跳管理类。 rpc服务把需要设置passing状态的serviceId注册到此类，
 * 此类会定时对注册的serviceId设置passing状态（实际是对serviceId对应对checkid设置passing状态），
 * 从而完成servivce的心跳。
 * 开关开启后会进行心跳，开关关闭则停止心跳。
 */
@Slf4j
public class ConsulHealthChecker {
    /**
     * 心跳周期，取ttl的2/3
     */
    public static    int                       HEARTBEAT_CIRCLE               = (TTL * 1000 * 2) / 3;
    /**
     * 连续检测开关变更的最大次数，超过这个次数就发送一次心跳
     */
    public static    int                       MAX_SWITCHER_CHECK_TIMES       = 10;
    /**
     * 检测开关变更的频率，连续检测MAX_SWITCHER_CHECK_TIMES次必须发送一次心跳。
     */
    public static    int                       SWITCHER_CHECK_CIRCLE          = HEARTBEAT_CIRCLE / MAX_SWITCHER_CHECK_TIMES;
    private final    LuixConsulClient          consulClient;
    private final    ScheduledExecutorService  heartbeatThreadPool;
    private final    ThreadPoolExecutor        jobExecutor;
    // 所有需要进行心跳的serviceId.
    private final    ConcurrentHashSet<String> serviceIds                     = new ConcurrentHashSet<>();
    // 上一次心跳开关的状态
    private          boolean                   lastHeartBeatSwitcherStatus    = false;
    private volatile boolean                   currentHeartBeatSwitcherStatus = false;
    // 开关检查次数。
    private          int                       switcherCheckTimes             = 0;

    public ConsulHealthChecker(LuixConsulClient consulClient) {
        this.consulClient = consulClient;
        heartbeatThreadPool = Executors.newSingleThreadScheduledExecutor();
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(10000);
        jobExecutor = new ThreadPoolExecutor(5, 30, 30 * 1000, TimeUnit.MILLISECONDS, workQueue);
    }

    public void start() {
        heartbeatThreadPool.scheduleAtFixedRate(
                () -> {
                    // 由于consul的check set pass会导致consul
                    // server的写磁盘操作，过于频繁的心跳会导致consul
                    // 性能问题，只能将心跳方式改为较长的周期进行一次探测。又因为想在关闭心跳开关后尽快感知
                    // 就将心跳改为以较小周期检测心跳开关是否变动，连续检测多次后给consul server发送一次心跳。
                    // TODO 改为开关listener方式。
                    try {
                        boolean switcherStatus = isHeartbeatOpen();
                        // 心跳开关状态变更
                        if (isSwitcherChange(switcherStatus)) {
                            processHeartbeat(switcherStatus);
                        } else {
                            // 心跳开关状态未变更
                            if (switcherStatus) {// 开关为开启状态，则连续检测超过MAX_SWITCHER_CHECK_TIMES次发送一次心跳
                                switcherCheckTimes++;
                                if (switcherCheckTimes >= MAX_SWITCHER_CHECK_TIMES) {
                                    processHeartbeat(true);
                                    switcherCheckTimes = 0;
                                }
                            }
                        }

                    } catch (Exception e) {
                        log.error("consul heartbeat executor err:", e);
                    }
                }, SWITCHER_CHECK_CIRCLE, SWITCHER_CHECK_CIRCLE, TimeUnit.MILLISECONDS);
    }

    /**
     * 判断心跳开关状态是否改变，如果心跳开关改变则更新lastHeartBeatSwitcherStatus为最新状态
     *
     * @param switcherStatus
     * @return
     */
    private boolean isSwitcherChange(boolean switcherStatus) {
        boolean ret = false;
        if (switcherStatus != lastHeartBeatSwitcherStatus) {
            ret = true;
            lastHeartBeatSwitcherStatus = switcherStatus;
            log.info("Consul heartbeat switcher change to [{}]", switcherStatus);
        }
        return ret;
    }

    protected void processHeartbeat(boolean isPass) {
        for (String serviceId : serviceIds) {
            try {
                jobExecutor.execute(new HeartbeatJob(serviceId, isPass));
            } catch (RejectedExecutionException ree) {
                log.error("Failed to execute heartbeat job with serviceId: [{}]", serviceId);
            }
        }
    }

    public void close() {
        heartbeatThreadPool.shutdown();
        jobExecutor.shutdown();
        log.info("Closed check consul heart manager");
    }

    /**
     * 添加consul serviceId，添加后的serviceId会通过定时设置passing状态保持心跳。
     *
     * @param serviceId
     */
    public void addCheckServiceId(String serviceId) {
        serviceIds.add(serviceId);
    }

    /**
     * 移除serviceId，对应的serviceId不会在进行心跳。
     *
     * @param serviceId
     */
    public void removeHeartbeatServiceId(String serviceId) {
        serviceIds.remove(serviceId);
    }

    /**
     * 检查心跳开关是否打开
     *
     * @return
     */
    private boolean isHeartbeatOpen() {
        return currentHeartBeatSwitcherStatus;
    }

    public void setHeartbeatOpen(boolean open) {
        currentHeartBeatSwitcherStatus = open;
    }

    class HeartbeatJob implements Runnable {
        private final String  serviceId;
        private final boolean isPass;

        public HeartbeatJob(String serviceId, boolean isPass) {
            super();
            this.serviceId = serviceId;
            this.isPass = isPass;
        }

        @Override
        public void run() {
            try {
                if (isPass) {
                    consulClient.checkPass(serviceId);
                } else {
                    consulClient.checkFail(serviceId);
                }
            } catch (Exception e) {
                log.error("consul heartbeat-set check pass error! serviceId:" + serviceId, e);
            }
        }
    }
}
