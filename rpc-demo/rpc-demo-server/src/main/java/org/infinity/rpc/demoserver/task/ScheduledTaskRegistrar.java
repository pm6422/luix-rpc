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
public class ScheduledTaskRegistrar implements DisposableBean {

    private static final Map<String, ScheduledTask> SCHEDULED_TASKS = new ConcurrentHashMap<>(16);
    /**
     * Spring task scheduler
     */
    @Resource
    private              TaskScheduler              taskScheduler;

    public void addCronTask(String name, Runnable task, String cronExpression) {
        Validate.notEmpty(name, "Task name must NOT be empty!");
        Validate.notNull(task, "Task must NOT be null!");
        Validate.notEmpty(cronExpression, "Cron expression must NOT be empty!");

        CronTask cronTask = new CronTask(task, cronExpression);
        this.SCHEDULED_TASKS.put(name, createCronTask(cronTask));
    }

    private ScheduledTask createCronTask(CronTask cronTask) {
        ScheduledFuture<?> scheduledFuture = this.taskScheduler.schedule(cronTask.getRunnable(), cronTask.getTrigger());
        return new ScheduledTask(scheduledFuture);
    }

    public void addFixedRateTask(String name, Runnable task, long interval) {
        Validate.notEmpty(name, "Task name must NOT be empty!");
        Validate.notNull(task, "Task must NOT be null!");

        this.SCHEDULED_TASKS.put(name, createFixedRateTask(task, interval));
    }

    private ScheduledTask createFixedRateTask(Runnable task, long period) {
        ScheduledFuture<?> scheduledFuture = this.taskScheduler.scheduleAtFixedRate(task, period);
        return new ScheduledTask(scheduledFuture);
    }

    public void removeTask(String name) {
        Validate.notEmpty(name, "Task name must NOT be empty!");

        ScheduledTask scheduledTask = this.SCHEDULED_TASKS.remove(name);
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