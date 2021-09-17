package org.infinity.luix.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.luix.demoserver.domain.ScheduledTask;
import org.infinity.luix.demoserver.service.ScheduledTaskService;
import org.infinity.luix.demoserver.task.schedule.CancelableScheduledTaskRegistrar;
import org.infinity.luix.demoserver.task.schedule.RunnableTask;
import org.infinity.luix.demoserver.exception.NoDataFoundException;
import org.infinity.luix.demoserver.repository.ScheduledTaskHistoryRepository;
import org.infinity.luix.demoserver.repository.ScheduledTaskLockRepository;
import org.infinity.luix.demoserver.repository.ScheduledTaskRepository;
import org.infinity.luix.utilities.id.IdGenerator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class ScheduledTaskServiceImpl implements ScheduledTaskService, ApplicationRunner {
    @Resource
    private ApplicationContext                applicationContext;
    @Resource
    private ScheduledTaskRepository           scheduledTaskRepository;
    @Resource
    private ScheduledTaskHistoryRepository    scheduledTaskHistoryRepository;
    @Resource
    private ScheduledTaskLockRepository      scheduledTaskLockRepository;
    @Resource
    private CancelableScheduledTaskRegistrar cancelableScheduledTaskRegistrar;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Timed task with normal state in initial load database
        List<ScheduledTask> enabledScheduledTasks = scheduledTaskRepository.findByEnabledIsTrue();
        if (CollectionUtils.isEmpty(enabledScheduledTasks)) {
            log.info("No scheduled tasks to execute!");
            return;
        }

        enabledScheduledTasks.forEach(this::addTask);
        log.info("Loaded all scheduled tasks");
    }

    @Override
    public ScheduledTask insert(ScheduledTask domain) {
        domain.setName("T" + IdGenerator.generateShortId());
        domain.setCreatedTime(Instant.now());
        ScheduledTask savedOne = scheduledTaskRepository.insert(domain);
        if (Boolean.TRUE.equals(savedOne.getEnabled())) {
            addTask(savedOne);
        }
        return savedOne;
    }

    @Override
    public void update(ScheduledTask domain) {
        ScheduledTask existingOne = scheduledTaskRepository.findById(domain.getId()).orElseThrow(() -> new NoDataFoundException(domain.getId()));
        if (Boolean.TRUE.equals(domain.getUseCronExpression())) {
            domain.setFixedInterval(null);
            domain.setFixedIntervalUnit(null);
        } else {
            domain.setCronExpression(null);
        }

        domain.setModifiedTime(Instant.now());
        ScheduledTask savedOne = scheduledTaskRepository.save(domain);

        // Remove before adding
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            removeTask(existingOne);
        }

        // Add a new one
        if (Boolean.TRUE.equals(savedOne.getEnabled())) {
            addTask(savedOne);
        }
    }

    @Override
    public void delete(String id) {
        ScheduledTask existingOne = scheduledTaskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        scheduledTaskRepository.deleteById(id);
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            removeTask(existingOne);
        }
    }

    @Override
    public void startOrStop(String id) {
        ScheduledTask existingOne = scheduledTaskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            addTask(existingOne);
        } else {
            removeTask(existingOne);
        }
    }

    @Override
    public Page<ScheduledTask> find(Pageable pageable, String name, String beanName) {
        ScheduledTask probe = new ScheduledTask();
        probe.setName(name);
        probe.setBeanName(beanName);
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        return scheduledTaskRepository.findAll(Example.of(probe, matcher), pageable);
    }

    private void addTask(ScheduledTask scheduledTask) {
        RunnableTask runnableTask = RunnableTask.builder()
                .applicationContext(applicationContext)
                .scheduledTaskHistoryRepository(scheduledTaskHistoryRepository)
                .scheduledTaskLockRepository(scheduledTaskLockRepository)
                .scheduledTask(scheduledTask)
                .build();
        if (Boolean.TRUE.equals(scheduledTask.getUseCronExpression())) {
            cancelableScheduledTaskRegistrar.addCronTask(scheduledTask.getName(), runnableTask, scheduledTask.getCronExpression());
        } else {
            cancelableScheduledTaskRegistrar.addFixedRateTask(scheduledTask.getName(), runnableTask, calculateMilliSeconds(scheduledTask));
        }
    }

    private void removeTask(ScheduledTask scheduledTask) {
        cancelableScheduledTaskRegistrar.removeTask(scheduledTask.getName());
    }

    private long calculateMilliSeconds(ScheduledTask scheduledTask) {
        long oneMinute = 60000;
        if (ScheduledTask.UNIT_MINUTES.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneMinute * scheduledTask.getFixedInterval();
        } else if (ScheduledTask.UNIT_HOURS.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneMinute * 60 * scheduledTask.getFixedInterval();
        } else if (ScheduledTask.UNIT_DAYS.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneMinute * 60 * 24 * scheduledTask.getFixedInterval();
        }
        throw new IllegalStateException("Found incorrect time unit!");
    }
}
