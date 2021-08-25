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

    private final Map<Runnable, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>(16);
    /**
     * Spring task scheduler
     */
    @Resource
    private       TaskScheduler                taskScheduler;

    public void addCronTask(Runnable task, String cronExpression) {
        Validate.notNull(task, "Task must NOT be null!");
        Validate.notEmpty(cronExpression, "Cron expression must NOT be empty!");

        CronTask cronTask = new CronTask(task, cronExpression);
        this.scheduledTasks.put(task, createScheduleTask(cronTask));
    }

    private ScheduledTask createScheduleTask(CronTask cronTask) {
        ScheduledFuture<?> scheduledFuture = this.taskScheduler.schedule(cronTask.getRunnable(), cronTask.getTrigger());
        return new ScheduledTask(scheduledFuture);
    }

    public void addFixedRateTask(Runnable task, long period) {
        Validate.notNull(task, "Task must NOT be null!");

        this.scheduledTasks.put(task, createFixedRateTask(task, period));
    }

    private ScheduledTask createFixedRateTask(Runnable task, long period) {
        ScheduledFuture<?> scheduledFuture = this.taskScheduler.scheduleAtFixedRate(task, period);
        return new ScheduledTask(scheduledFuture);
    }

    public void removeTask(Runnable task) {
        Validate.notNull(task, "Task must NOT be null!");

        ScheduledTask scheduledTask = this.scheduledTasks.remove(task);
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
    }

    @Override
    public void destroy() {
        if (MapUtils.isEmpty(scheduledTasks)) {
            return;
        }
        scheduledTasks.values().forEach(ScheduledTask::cancel);
        scheduledTasks.clear();
    }
}