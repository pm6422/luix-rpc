package org.infinity.luix.registry.consul;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.utilities.collection.ConcurrentHashSet;

import java.util.concurrent.*;

import static org.infinity.luix.registry.consul.ConsulService.TTL;

/**
 * consul 心跳管理类。 rpc服务把需要设置passing状态的serviceId注册到此类，
 * 此类会定时对注册的serviceId设置passing状态（实际是对serviceId对应对checkId设置passing状态），
 * 从而完成servivce的心跳。
 * 开关开启后会进行心跳，开关关闭则停止心跳。
 */
@Slf4j
public class ConsulHealthChecker {
    /**
     * 心跳周期，取ttl的2/3
     */
    private static   int                       HEARTBEAT_CIRCLE                 = (TTL * 1000 * 2) / 3;
    /**
     * 连续检测开关变更的最大次数，超过这个次数就发送一次心跳
     */
    private static   int                       MAX_SWITCHER_CHECK_TIMES         = 10;
    /**
     * 检测开关变更的频率，连续检测MAX_SWITCHER_CHECK_TIMES次必须发送一次心跳。
     */
    private static   int                       SWITCHER_CHECK_CIRCLE            = HEARTBEAT_CIRCLE / MAX_SWITCHER_CHECK_TIMES;
    /**
     * Luix consul client
     */
    private final    LuixConsulClient          consulClient;
    /**
     * Check health scheduling thread pool
     */
    private final    ScheduledExecutorService  checkHealthSchedulingThreadPool;
    /**
     * Check health execution thread pool
     */
    private final    ThreadPoolExecutor        checkHealthThreadPool;
    /**
     * Service instance IDs that need to be health checked.
     */
    private final    ConcurrentHashSet<String> checkingServiceInstanceIds       = new ConcurrentHashSet<>();
    /**
     * Previous check health switcher status
     */
    private          boolean                   prevCheckHealthSwitcherStatus    = false;
    /**
     * Current check health switcher status
     */
    private volatile boolean                   currentCheckHealthSwitcherStatus = false;
    /**
     * Switcher check times
     */
    private          int                       switcherCheckTimes               = 0;

    public ConsulHealthChecker(LuixConsulClient consulClient) {
        this.consulClient = consulClient;
        checkHealthSchedulingThreadPool = Executors.newSingleThreadScheduledExecutor();
        checkHealthThreadPool = createCheckHealthThreadPool();
    }

    private ThreadPoolExecutor createCheckHealthThreadPool() {
        return new ThreadPoolExecutor(5, 30, 30 * 1_000,
                TimeUnit.MILLISECONDS, createWorkQueue());
    }

    private BlockingQueue<Runnable> createWorkQueue() {
        return new ArrayBlockingQueue<>(10_000);
    }

    /**
     * Add consul service instance ID, add the service instance ID will keep the heartbeat 'passing' status by a timer.
     *
     * @param serviceInstanceId service instance ID
     */
    public void addCheckingServiceInstanceId(String serviceInstanceId) {
        checkingServiceInstanceIds.add(serviceInstanceId);
    }

    /**
     * Remove consul service instance ID, remove the service instance ID will not keep the heartbeat 'passing' status by a timer.
     *
     * @param serviceInstanceId service instance ID
     */
    public void removeCheckingServiceInstanceId(String serviceInstanceId) {
        checkingServiceInstanceIds.remove(serviceInstanceId);
    }

    /**
     * Determine whether the check health switcher status is changed.
     *
     * @param currentCheckHealthSwitcherStatus current check health switcher status
     * @return true if check health switcher status changed, false otherwise
     */
    private boolean isSwitcherStatusChange(boolean currentCheckHealthSwitcherStatus) {
        boolean result = false;
        if (currentCheckHealthSwitcherStatus != prevCheckHealthSwitcherStatus) {
            result = true;
            prevCheckHealthSwitcherStatus = currentCheckHealthSwitcherStatus;
            log.info("Changed consul check health switcher value to [{}]", currentCheckHealthSwitcherStatus);
        }
        return result;
    }

    public void setCheckHealthSwitcherStatus(boolean checkHealthSwitcherStatus) {
        currentCheckHealthSwitcherStatus = checkHealthSwitcherStatus;
    }

    public void start() {
        checkHealthSchedulingThreadPool.scheduleAtFixedRate(
                () -> {
                    // 由于consul的check set pass会导致consul
                    // server的写磁盘操作，过于频繁的心跳会导致consul
                    // 性能问题，只能将心跳方式改为较长的周期进行一次探测。又因为想在关闭心跳开关后尽快感知
                    // 就将心跳改为以较小周期检测心跳开关是否变动，连续检测多次后给consul server发送一次心跳。
                    // TODO 改为开关listener方式。
                    try {
                        boolean switcherStatus = currentCheckHealthSwitcherStatus;
                        if (isSwitcherStatusChange(switcherStatus)) {
                            // 心跳开关状态已变更
                            checkHealth(switcherStatus);
                        } else {
                            // 心跳开关状态未变更
                            if (switcherStatus) {
                                // 开关为开启状态，则连续检测超过MAX_SWITCHER_CHECK_TIMES次发送一次心跳
                                switcherCheckTimes++;
                                if (switcherCheckTimes >= MAX_SWITCHER_CHECK_TIMES) {
                                    checkHealth(true);
                                    switcherCheckTimes = 0;
                                }
                            }
                        }

                    } catch (Exception e) {
                        log.error("consul heartbeat executor err:", e);
                    }
                }, SWITCHER_CHECK_CIRCLE, SWITCHER_CHECK_CIRCLE, TimeUnit.MILLISECONDS);
    }

    protected void checkHealth(boolean checkPass) {
        for (String instanceId : checkingServiceInstanceIds) {
            try {
                checkHealthThreadPool.execute(new CheckHealthJob(instanceId, checkPass));
            } catch (RejectedExecutionException ree) {
                log.error("Failed to execute health checking job with consul service instance ID: [{}]", instanceId);
            }
        }
    }

    public void close() {
        checkHealthSchedulingThreadPool.shutdown();
        checkHealthThreadPool.shutdown();
        log.info("Closed consul check health manager");
    }

    class CheckHealthJob implements Runnable {
        private final String  serviceInstanceId;
        private final boolean checkPass;

        public CheckHealthJob(String serviceInstanceId, boolean checkPass) {
            super();
            this.serviceInstanceId = serviceInstanceId;
            this.checkPass = checkPass;
        }

        @Override
        public void run() {
            try {
                if (checkPass) {
                    // 设置一个本地检查项的状态为passing
                    consulClient.checkPass(serviceInstanceId);
                } else {
                    // 设置一个本地检查项的状态为critical
                    consulClient.checkFail(serviceInstanceId);
                }
            } catch (Exception e) {
                log.error("Failed to set consul checker with consul service instance ID: [" + serviceInstanceId + "]");
            }
        }
    }
}
