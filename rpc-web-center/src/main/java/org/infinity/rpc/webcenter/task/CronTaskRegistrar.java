package org.infinity.rpc.webcenter.task;

import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CronTaskRegistrar implements DisposableBean {

    private final Map<Runnable, CancellableScheduledTask> scheduledTasks = new ConcurrentHashMap<>(16);
    @Resource
    private       TaskScheduler                           taskScheduler;

    public TaskScheduler getScheduler() {
        return this.taskScheduler;
    }

    public void addCronTask(Runnable task, String cronExpression) {
        addCronTask(new CronTask(task, cronExpression));
    }

    public void addCronTask(CronTask cronTask) {
        if (cronTask != null) {
            Runnable task = cronTask.getRunnable();
            if (this.scheduledTasks.containsKey(task)) {
                removeCronTask(task);
            }
            this.scheduledTasks.put(task, scheduleCronTask(cronTask));
        }
    }

    public void removeCronTask(Runnable task) {
        CancellableScheduledTask cancellableScheduledTask = this.scheduledTasks.remove(task);
        if (cancellableScheduledTask != null) {
            cancellableScheduledTask.cancel();
        }
    }

    public CancellableScheduledTask scheduleCronTask(CronTask cronTask) {
        CancellableScheduledTask cancellableScheduledTask = new CancellableScheduledTask();
        cancellableScheduledTask.future = this.taskScheduler.schedule(cronTask.getRunnable(), cronTask.getTrigger());
        return cancellableScheduledTask;
    }

    @Override
    public void destroy() {
        if (MapUtils.isEmpty(scheduledTasks)) {
            return;
        }
        scheduledTasks.values().forEach(CancellableScheduledTask::cancel);
        scheduledTasks.clear();
    }
}