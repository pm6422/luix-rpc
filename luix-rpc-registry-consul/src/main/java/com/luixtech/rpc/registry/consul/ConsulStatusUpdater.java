package com.luixtech.rpc.registry.consul;

import com.luixtech.utilities.lang.collection.ConcurrentHashSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.luixtech.rpc.registry.consul.ConsulService.TTL;

/**
 * When the switch is turned on, the heartbeat will occur, and when the switch is turned off, the heartbeat will stop.
 */
@Slf4j
public class ConsulStatusUpdater {
    /**
     * 心跳周期，取TTL的2/3
     */
    private static final int                       HEARTBEAT_CIRCLE    = (TTL * 1000 * 2) / 3;
    /**
     * 连续检测开关变更的最大次数，超过这个次数就发送一次心跳
     */
    private static final int                       MAX_CHECK_TIMES     = 10;
    /**
     * 连续检测MAX_CHECK_TIMES次必须发送一次心跳
     */
    private static final int                       SCHEDULE_INTERVAL   = HEARTBEAT_CIRCLE / MAX_CHECK_TIMES;
    /**
     * Luix consul client
     */
    private final        ConsulHttpClient          consulClient;
    /**
     * Consul service instance status update executor service
     */
    private final        ScheduledExecutorService  statusUpdateExecutorService;
    /**
     * Consul service instance status update execution thread pool
     */
    private final        ThreadPoolExecutor        executionThreadPool;
    /**
     * Service instance IDs that need to be health checked.
     */
    private final        ConcurrentHashSet<String> checkingInstanceIds = new ConcurrentHashSet<>();
    /**
     * Status of current consul service instances
     */
    private              AtomicBoolean             active              = new AtomicBoolean(false);
    /**
     * Check times
     */
    private              AtomicInteger             checkTimes          = new AtomicInteger(0);

    public ConsulStatusUpdater(ConsulHttpClient consulClient) {
        this.consulClient = consulClient;
        statusUpdateExecutorService = Executors.newSingleThreadScheduledExecutor();
        executionThreadPool = createExecutionThreadPool();
    }

    private ThreadPoolExecutor createExecutionThreadPool() {
        return new ThreadPoolExecutor(5, 30, 30 * 1_000,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10_000), new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Add consul service instance ID
     *
     * @param serviceInstanceId service instance ID
     */
    private void addInstanceId(String serviceInstanceId) {
        checkingInstanceIds.add(serviceInstanceId);
    }

    /**
     * Remove consul service instance ID
     *
     * @param serviceInstanceId service instance ID
     */
    private void removeInstanceId(String serviceInstanceId) {
        checkingInstanceIds.remove(serviceInstanceId);
    }

    /**
     * Update consul service instance status
     *
     * @param active consul service instance status
     */
    public void updateStatus(boolean active) {
        if (this.active.compareAndSet(!active, active)) {
            doUpdateStatus(active);
            log.info("Changed consul service instance status to [{}]", active);
        }
    }

    /**
     * Set all status of service instance to 'passing' or 'critical' by sending a REST request to consul server
     *
     * @param active consul service instance status
     */
    private void doUpdateStatus(boolean active) {
        for (String instanceId : checkingInstanceIds) {
            try {
                executionThreadPool.execute(() -> {
                    if (active) {
                        activate(instanceId);
                    } else {
                        deactivate(instanceId);
                    }
                });
            } catch (RejectedExecutionException e) {
                log.error("Failed to execute health checking task with consul service instance ID: [" + instanceId + "]", e);
            }
        }
    }

    /**
     * Set the status of service instance to 'passing' by sending a REST request to consul server
     *
     * @param serviceInstanceId service instance ID
     */
    public void activate(String serviceInstanceId) {
        addInstanceId(serviceInstanceId);
        if (CollectionUtils.isNotEmpty(checkingInstanceIds)) {
            updateStatus(true);
        }
        consulClient.activate(serviceInstanceId);
    }

    /**
     * Set the status of service instance to 'failing' by sending a REST request to consul server
     *
     * @param serviceInstanceId service instance ID
     */
    public void deactivate(String serviceInstanceId) {
        removeInstanceId(serviceInstanceId);
        if (CollectionUtils.isEmpty(checkingInstanceIds)) {
            updateStatus(false);
        }
        consulClient.deactivate(serviceInstanceId);
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
        // Periodically set status of consul service instance to 'passing' for the registered service instance ID
        statusUpdateExecutorService.scheduleAtFixedRate(
                () -> {
                    if (this.active.get()) {
                        if (checkTimes.incrementAndGet() >= MAX_CHECK_TIMES) {
                            doUpdateStatus(true);
                            checkTimes.set(0);
                        }
                    }
                }, SCHEDULE_INTERVAL, SCHEDULE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop the scheduled task
     */
    public void close() {
        statusUpdateExecutorService.shutdown();
        executionThreadPool.shutdown();
        log.info("Closed consul service instance status updater");
    }
}
