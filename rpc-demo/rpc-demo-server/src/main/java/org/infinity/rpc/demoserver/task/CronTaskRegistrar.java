package org.infinity.rpc.demoserver.task;

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
public class CronTaskRegistrar implements DisposableBean {

    private static final Map<Runnable, ScheduledTask> SCHEDULED_TASKS = new ConcurrentHashMap<>(16);
    /**
     * Spring task scheduler
     */
    @Resource
    private              TaskScheduler                taskScheduler;

    public void addCronTask(Runnable task, String cronExpression) {
        Validate.notNull(task, "Task must NOT be null!");
        Validate.notEmpty(cronExpression, "Cron expression must NOT be empty!");

        CronTask cronTask = new CronTask(task, cronExpression);
        this.SCHEDULED_TASKS.put(task, createCronTask(cronTask));
    }

    private ScheduledTask createCronTask(CronTask cronTask) {
        ScheduledFuture<?> scheduledFuture = this.taskScheduler.schedule(cronTask.getRunnable(), cronTask.getTrigger());
        return new ScheduledTask(scheduledFuture);
    }

    public void addFixedRateTask(Runnable task, long interval) {
        Validate.notNull(task, "Task must NOT be null!");

        this.SCHEDULED_TASKS.put(task, createFixedRateTask(task, interval));
    }

    private ScheduledTask createFixedRateTask(Runnable task, long period) {
        ScheduledFuture<?> scheduledFuture = this.taskScheduler.scheduleAtFixedRate(task, period);
        return new ScheduledTask(scheduledFuture);
    }

    public void removeTask(Runnable task) {
        Validate.notNull(task, "Task must NOT be null!");

        ScheduledTask scheduledTask = this.SCHEDULED_TASKS.remove(task);
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
    }

    @Override
    public void destroy() {
        if (MapUtils.isEmpty(SCHEDULED_TASKS)) {
            return;
        }
        SCHEDULED_TASKS.values().forEach(ScheduledTask::cancel);
        SCHEDULED_TASKS.clear();
    }
}