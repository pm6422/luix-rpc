package org.infinity.rpc.webcenter.task;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class ScheduledTaskRegistrar implements DisposableBean {

    private static final Map<String, CancellableScheduledTask> SCHEDULED_TASKS = new ConcurrentHashMap<>(16);
    /**
     * Spring task scheduler
     */
    @Resource
    private              TaskScheduler                         taskScheduler;

    public synchronized void addCronTask(String name, Runnable task, String cronExpression) {
        Validate.notEmpty(name, "Task name must NOT be empty!");
        Validate.notNull(task, "Task must NOT be null!");
        Validate.notEmpty(cronExpression, "Cron expression must NOT be empty!");

        CronTask cronTask = new CronTask(task, cronExpression);
        this.SCHEDULED_TASKS.put(name, createCronTask(cronTask));
    }

    private CancellableScheduledTask createCronTask(CronTask cronTask) {
        ScheduledFuture<?> scheduledFuture = this.taskScheduler.schedule(cronTask.getRunnable(), cronTask.getTrigger());
        return new CancellableScheduledTask(scheduledFuture);
    }

    public synchronized void addFixedRateTask(String name, Runnable task, long interval) {
        Validate.notEmpty(name, "Task name must NOT be empty!");
        Validate.notNull(task, "Task must NOT be null!");

        this.SCHEDULED_TASKS.put(name, createFixedRateTask(task, interval));
    }

    private CancellableScheduledTask createFixedRateTask(Runnable task, long period) {
        ScheduledFuture<?> scheduledFuture = this.taskScheduler.scheduleAtFixedRate(task, period);
        return new CancellableScheduledTask(scheduledFuture);
    }

    public synchronized void removeTask(String name) {
        Validate.notEmpty(name, "Task name must NOT be empty!");

        CancellableScheduledTask cancellableScheduledTask = this.SCHEDULED_TASKS.remove(name);
        if (cancellableScheduledTask != null) {
            cancellableScheduledTask.cancel();
        }
    }

    @Override
    public void destroy() {
        if (MapUtils.isEmpty(SCHEDULED_TASKS)) {
            return;
        }
        SCHEDULED_TASKS.values().forEach(CancellableScheduledTask::cancel);
        SCHEDULED_TASKS.clear();
    }
}