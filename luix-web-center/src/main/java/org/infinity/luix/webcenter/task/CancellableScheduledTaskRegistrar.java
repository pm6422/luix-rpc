package org.infinity.luix.webcenter.task;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * It has similar functions to {@link org.springframework.scheduling.config.ScheduledTaskRegistrar},
 * and it can also cancel the started scheduled task.
 */
@Component
public class CancellableScheduledTaskRegistrar implements DisposableBean {

    private static final Map<String, ScheduledFuture<?>> SCHEDULED_TASKS = new ConcurrentHashMap<>(16);
    /**
     * Spring task scheduler
     */
    @Resource
    private              TaskScheduler                   taskScheduler;

    public synchronized void addCronTask(String id, Runnable task, String cronExpression) {
        Validate.notEmpty(id, "Task ID must NOT be empty!");
        Validate.notNull(task, "Task must NOT be null!");
        Validate.notEmpty(cronExpression, "Cron expression must NOT be empty!");

        SCHEDULED_TASKS.put(id, this.taskScheduler.schedule(task, new CronTrigger(cronExpression)));
    }

    public synchronized void addFixedRateTask(String id, Runnable task, long interval) {
        Validate.notEmpty(id, "Task ID must NOT be empty!");
        Validate.notNull(task, "Task must NOT be null!");

        SCHEDULED_TASKS.put(id, this.taskScheduler.scheduleAtFixedRate(task, interval));
    }

    public synchronized void removeTask(String id) {
        Validate.notEmpty(id, "Task ID must NOT be empty!");

        Optional.ofNullable(SCHEDULED_TASKS.remove(id)).ifPresent(future -> future.cancel(true));
    }

    @Override
    public void destroy() {
        if (MapUtils.isEmpty(SCHEDULED_TASKS)) {
            return;
        }
        SCHEDULED_TASKS.values().forEach(future -> future.cancel(true));
        SCHEDULED_TASKS.clear();
    }
}