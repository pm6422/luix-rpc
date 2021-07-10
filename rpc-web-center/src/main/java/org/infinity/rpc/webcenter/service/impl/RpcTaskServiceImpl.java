package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.infinity.rpc.webcenter.domain.RpcTask;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcTaskHistoryRepository;
import org.infinity.rpc.webcenter.repository.RpcTaskLockRepository;
import org.infinity.rpc.webcenter.repository.RpcTaskRepository;
import org.infinity.rpc.webcenter.service.RpcTaskService;
import org.infinity.rpc.webcenter.task.CronTaskRegistrar;
import org.infinity.rpc.webcenter.task.TaskRunnable;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RpcTaskServiceImpl implements RpcTaskService, ApplicationRunner {
    @Resource
    private RpcTaskRepository        taskRepository;
    @Resource
    private RpcTaskHistoryRepository taskHistoryRepository;
    @Resource
    private RpcTaskLockRepository    taskLockRepository;
    @Resource
    private CronTaskRegistrar        cronTaskRegistrar;

    @Override
    public void refresh() throws Exception {
        cronTaskRegistrar.destroy();
        run(null);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Timed task with normal state in initial load database
        List<RpcTask> enabledTasks = taskRepository.findByEnabledIsTrue();
        if (CollectionUtils.isEmpty(enabledTasks)) {
            log.info("No tasks to execute!");
            return;
        }

        for (RpcTask task : enabledTasks) {
            TaskRunnable runnable = TaskRunnable.builder()
                    .taskHistoryRepository(taskHistoryRepository)
                    .taskLockRepository(taskLockRepository)
                    .name(task.getName())
                    .beanName(task.getBeanName())
                    .argumentsJson(task.getArgumentsJson())
                    .cronExpression(task.getCronExpression())
                    .allHostsRun(task.isAllHostsRun())
                    .build();
            cronTaskRegistrar.addCronTask(runnable, task.getCronExpression());
        }
        log.info("Loaded all tasks");
    }

    @Override
    public RpcTask insert(RpcTask domain) {
        domain.setName("T" + IdGenerator.generateShortId());
        RpcTask savedOne = taskRepository.save(domain);
        if (Boolean.TRUE.equals(savedOne.getEnabled())) {
            TaskRunnable runnable = TaskRunnable.builder()
                    .taskHistoryRepository(taskHistoryRepository)
                    .taskLockRepository(taskLockRepository)
                    .name(savedOne.getName())
                    .beanName(savedOne.getBeanName())
                    .argumentsJson(savedOne.getArgumentsJson())
                    .cronExpression(savedOne.getCronExpression())
                    .allHostsRun(savedOne.isAllHostsRun())
                    .build();
            cronTaskRegistrar.addCronTask(runnable, savedOne.getCronExpression());
        }
        return savedOne;
    }

    @Override
    public void update(RpcTask domain) {
        RpcTask existingOne = taskRepository.findById(domain.getId()).orElseThrow(() -> new NoDataFoundException(domain.getId()));
        RpcTask savedOne = taskRepository.save(domain);

        // Remove before adding
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            TaskRunnable runnable = TaskRunnable.builder()
                    .taskHistoryRepository(taskHistoryRepository)
                    .taskLockRepository(taskLockRepository)
                    .name(existingOne.getName())
                    .beanName(existingOne.getBeanName())
                    .argumentsJson(existingOne.getArgumentsJson())
                    .cronExpression(existingOne.getCronExpression())
                    .allHostsRun(existingOne.isAllHostsRun())
                    .build();
            cronTaskRegistrar.removeCronTask(runnable);
        }

        // Add a new one
        if (Boolean.TRUE.equals(savedOne.getEnabled())) {
            TaskRunnable runnable = TaskRunnable.builder()
                    .taskHistoryRepository(taskHistoryRepository)
                    .taskLockRepository(taskLockRepository)
                    .name(savedOne.getName())
                    .beanName(savedOne.getBeanName())
                    .argumentsJson(savedOne.getArgumentsJson())
                    .cronExpression(savedOne.getCronExpression())
                    .allHostsRun(savedOne.isAllHostsRun())
                    .build();
            cronTaskRegistrar.addCronTask(runnable, domain.getCronExpression());
        }
    }

    @Override
    public void delete(String id) {
        RpcTask existingOne = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        taskRepository.deleteById(id);
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            TaskRunnable runnable = TaskRunnable.builder()
                    .taskHistoryRepository(taskHistoryRepository)
                    .taskLockRepository(taskLockRepository)
                    .name(existingOne.getName())
                    .beanName(existingOne.getBeanName())
                    .argumentsJson(existingOne.getArgumentsJson())
                    .cronExpression(existingOne.getCronExpression())
                    .allHostsRun(existingOne.isAllHostsRun())
                    .build();
            cronTaskRegistrar.removeCronTask(runnable);
        }
    }

    @Override
    public void startOrPause(String id) {
        RpcTask existingOne = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        TaskRunnable runnable = TaskRunnable.builder()
                .taskHistoryRepository(taskHistoryRepository)
                .taskLockRepository(taskLockRepository)
                .name(existingOne.getName())
                .beanName(existingOne.getBeanName())
                .argumentsJson(existingOne.getArgumentsJson())
                .cronExpression(existingOne.getCronExpression())
                .allHostsRun(existingOne.isAllHostsRun())
                .build();
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            cronTaskRegistrar.addCronTask(runnable, existingOne.getCronExpression());
        } else {
            cronTaskRegistrar.removeCronTask(runnable);
        }
    }

    @Override
    public Page<RpcTask> find(Pageable pageable, String name, String beanName, String methodName) {
        RpcTask probe = new RpcTask();
        probe.setName(name);
        probe.setBeanName(beanName);
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        return taskRepository.findAll(Example.of(probe, matcher), pageable);
    }
}
