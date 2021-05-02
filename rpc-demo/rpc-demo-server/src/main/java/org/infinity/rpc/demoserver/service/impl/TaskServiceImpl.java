package org.infinity.rpc.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.demoserver.domain.Task;
import org.infinity.rpc.demoserver.exception.NoDataFoundException;
import org.infinity.rpc.demoserver.repository.TaskHistoryRepository;
import org.infinity.rpc.demoserver.repository.TaskRepository;
import org.infinity.rpc.demoserver.service.TaskService;
import org.infinity.rpc.demoserver.task.CronTaskRegistrar;
import org.infinity.rpc.demoserver.task.TaskRunnable;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService, ApplicationRunner {
    @Resource
    private TaskRepository        taskRepository;
    @Resource
    private TaskHistoryRepository taskHistoryRepository;
    @Resource
    private CronTaskRegistrar     cronTaskRegistrar;

    @Override
    public void refresh() throws Exception {
        cronTaskRegistrar.destroy();
        run(null);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Timed task with normal state in initial load database
        List<Task> enabledTasks = taskRepository.findByEnabledIsTrue();
        if (CollectionUtils.isEmpty(enabledTasks)) {
            log.info("No tasks to execute!");
            return;
        }

        for (Task task : enabledTasks) {
            TaskRunnable runnable = TaskRunnable.builder()
                    .taskHistoryRepository(taskHistoryRepository)
                    .task(task)
                    .build();
            cronTaskRegistrar.addCronTask(runnable, task.getCronExpression());
        }
        log.info("Loaded all tasks");
    }

    @Override
    public Task insert(Task domain) {
        domain.setName("T" + IdGenerator.generateShortId());
        Task saved = taskRepository.save(domain);
        if (Boolean.TRUE.equals(saved.getEnabled())) {
            TaskRunnable runnable = TaskRunnable.builder()
                    .taskHistoryRepository(taskHistoryRepository)
                    .task(saved)
                    .build();
            cronTaskRegistrar.addCronTask(runnable, saved.getCronExpression());
        }
        return saved;
    }

    @Override
    public void update(Task domain) {
        Task existingOne = taskRepository.findById(domain.getId()).orElseThrow(() -> new NoDataFoundException(domain.getId()));
        Task saved = taskRepository.save(domain);

        // Remove before adding
        if (Boolean.TRUE.equals(saved.getEnabled())) {
            TaskRunnable runnable = TaskRunnable.builder()
                    .taskHistoryRepository(taskHistoryRepository)
                    .task(existingOne)
                    .build();
            cronTaskRegistrar.removeCronTask(runnable);
        }

        // Add a new one
        if (Boolean.TRUE.equals(saved.getEnabled())) {
            TaskRunnable runnable = TaskRunnable.builder()
                    .taskHistoryRepository(taskHistoryRepository)
                    .task(saved)
                    .build();
            cronTaskRegistrar.addCronTask(runnable, domain.getCronExpression());
        }
    }

    @Override
    public void delete(String id) {
        Task existingOne = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        taskRepository.deleteById(id);
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            TaskRunnable runnable = TaskRunnable.builder()
                    .taskHistoryRepository(taskHistoryRepository)
                    .task(existingOne)
                    .build();
            cronTaskRegistrar.removeCronTask(runnable);
        }
    }

    @Override
    public void startOrPause(String id) {
        Task existingOne = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        TaskRunnable runnable = TaskRunnable.builder()
                .taskHistoryRepository(taskHistoryRepository)
                .task(existingOne)
                .build();
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            cronTaskRegistrar.addCronTask(runnable, existingOne.getCronExpression());
        } else {
            cronTaskRegistrar.removeCronTask(runnable);
        }
    }
}
