package org.infinity.luix.registry.consul;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.utilities.collection.ConcurrentHashSet;

import java.util.concurrent.*;

import static org.infinity.luix.registry.consul.ConsulService.TTL;

/**
 * When the switch is turned on, the heartbeat will occur, and when the switch is turned off, the heartbeat will stop.
 */
@Slf4j
public class ConsulServiceStatusUpdater {
    /**
     * 心跳周期，取ttl的2/3
     */
    private static final int                       HEARTBEAT_CIRCLE           = (TTL * 1000 * 2) / 3;
    /**
     * 连续检测开关变更的最大次数，超过这个次数就发送一次心跳
     */
    private static final int                       MAX_CHECK_TIMES            = 10;
    /**
     * 检测开关变更的频率，连续检测MAX_SWITCHER_CHECK_TIMES次必须发送一次心跳。
     */
    private static final int                       CHECK_SCHEDULE_INTERVAL    = HEARTBEAT_CIRCLE / MAX_CHECK_TIMES;
    /**
     * Consul service instance 'passing' status
     */
    public static final  String                    STATUS_PASSING             = "passing";
    /**
     * Consul service instance 'failing' status
     */
    public static final  String                    STATUS_FAILING             = "failing";
    /**
     * Status of previous consul service instances
     */
    private              String                    prevStatus                 = STATUS_FAILING;
    /**
     * Status of current consul service instances
     */
    private volatile     String                    currentStatus              = STATUS_FAILING;
    /**
     * Luix consul client
     */
    private final        LuixConsulClient          consulClient;
    /**
     * Check health scheduling thread pool
     */
    private final        ScheduledExecutorService  checkHealthSchedulingThreadPool;
    /**
     * Check health execution thread pool
     */
    private final        ThreadPoolExecutor        checkHealthThreadPool;
    /**
     * Service instance IDs that need to be health checked.
     */
    private final        ConcurrentHashSet<String> checkingServiceInstanceIds = new ConcurrentHashSet<>();
    /**
     * Switcher check times
     */
    private              int                       checkTimes                 = 0;

    public ConsulServiceStatusUpdater(LuixConsulClient consulClient) {
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
    public void addInstanceId(String serviceInstanceId) {
        checkingServiceInstanceIds.add(serviceInstanceId);
    }

    /**
     * Remove consul service instance ID, remove the service instance ID will not keep the heartbeat 'passing' status by a timer.
     *
     * @param serviceInstanceId service instance ID
     */
    public void removeInstanceId(String serviceInstanceId) {
        checkingServiceInstanceIds.remove(serviceInstanceId);
    }

    public void updateStatus(String status) {
        currentStatus = status;
        if (!currentStatus.equals(prevStatus)) {
            prevStatus = currentStatus;
            doSetServiceInstanceStatus(currentStatus);
            log.info("Changed consul service instance status to [{}]", currentStatus);
        }
    }

    /**
     * Set the status of service instance to 'passing' by sending a REST request to consul server
     *
     * @param serviceInstanceId service instance ID
     */
    public void activate(String serviceInstanceId) {
        consulClient.checkPass(serviceInstanceId);
    }

    /**
     * Set the status of service instance to 'failing' by sending a REST request to consul server
     *
     * @param serviceInstanceId service instance ID
     */
    public void deactivate(String serviceInstanceId) {
        consulClient.checkFail(serviceInstanceId);
    }

    /**
     * Each consul service instance will be registered a TTL type check. We can prolong the TTL lifecycle by a timer.
     * Set check pass to consul can cause disk writing operation of consul server,
     * too frequent heartbeat will cause performance problems of the consul server.
     * The heartbeat mode can only be changed to a longer cycle for one detection.
     * Because we want to sense the heartbeat as soon as possible after turning off the heartbeat switch,
     * change the heartbeat to detect whether the heartbeat switch changes in a small cycle,
     * and send a heartbeat to the consumer server after continuous detection for many times.
     */
    public void start() {
        checkHealthSchedulingThreadPool.scheduleAtFixedRate(
                () -> {
                    if (STATUS_PASSING.equals(currentStatus)) {
                        // 开关为开启状态，则连续检测超过MAX_SWITCHER_CHECK_TIMES次发送一次心跳
                        checkTimes++;
                        if (checkTimes >= MAX_CHECK_TIMES) {
                            // Periodically set status of consul service instance to 'passing' for the registered service instance ID
                            doSetServiceInstanceStatus(STATUS_PASSING);
                            checkTimes = 0;
                        }
                    }
                }, CHECK_SCHEDULE_INTERVAL, CHECK_SCHEDULE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    protected void doSetServiceInstanceStatus(String status) {
        for (String instanceId : checkingServiceInstanceIds) {
            try {
                checkHealthThreadPool.execute(new CheckHealthJob(instanceId, status));
            } catch (RejectedExecutionException ree) {
                log.error("Failed to execute health checking job with consul service instance ID: [" + instanceId + "]", ree);
            }
        }
    }

    public void close() {
        checkHealthSchedulingThreadPool.shutdown();
        checkHealthThreadPool.shutdown();
        log.info("Closed consul service instance health checker");
    }

    class CheckHealthJob implements Runnable {
        private final String serviceInstanceId;
        private final String status;

        public CheckHealthJob(String serviceInstanceId, String status) {
            super();
            this.serviceInstanceId = serviceInstanceId;
            this.status = status;
        }

        @Override
        public void run() {
            try {
                if (STATUS_PASSING.equals(status)) {
                    activate(serviceInstanceId);
                } else {
                    deactivate(serviceInstanceId);
                }
            } catch (Exception e) {
                log.error("Failed to set the status of consul service instance with ID: [" + serviceInstanceId + "]", e);
            }
        }
    }
}
