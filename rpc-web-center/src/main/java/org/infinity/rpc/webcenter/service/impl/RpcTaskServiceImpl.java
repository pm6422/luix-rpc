package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.infinity.rpc.webcenter.domain.RpcTask;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcTaskHistoryRepository;
import org.infinity.rpc.webcenter.repository.RpcTaskLockRepository;
import org.infinity.rpc.webcenter.repository.RpcTaskRepository;
import org.infinity.rpc.webcenter.service.RpcRegistryService;
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
    private RpcRegistryService       rpcRegistryService;
    @Resource
    private CronTaskRegistrar        cronTaskRegistrar;
    @Resource
    private InfinityProperties       infinityProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Timed task with normal state in initial load database
        List<RpcTask> enabledTasks = taskRepository.findByEnabledIsTrue();
        if (CollectionUtils.isEmpty(enabledTasks)) {
            log.info("No tasks to execute!");
            return;
        }

        for (RpcTask task : enabledTasks) {
            TaskRunnable runnable = createTaskRunnable(task);
            cronTaskRegistrar.addCronTask(runnable, task.getCronExpression());
        }
        log.info("Loaded all tasks");
    }

    @Override
    public void refresh() throws Exception {
        cronTaskRegistrar.destroy();
        run(null);
    }

    @Override
    public RpcTask insert(RpcTask domain) {
        domain.setName("T" + IdGenerator.generateShortId());
        RpcTask savedTask = taskRepository.save(domain);
        if (Boolean.TRUE.equals(savedTask.getEnabled())) {
            TaskRunnable runnable = createTaskRunnable(savedTask);
            cronTaskRegistrar.addCronTask(runnable, savedTask.getCronExpression());
        }
        return savedTask;
    }

    @Override
    public void update(RpcTask domain) {
        RpcTask existingOne = taskRepository.findById(domain.getId()).orElseThrow(() -> new NoDataFoundException(domain.getId()));
        RpcTask savedOne = taskRepository.save(domain);

        // Remove before adding
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            TaskRunnable runnable = createTaskRunnable(existingOne);
            cronTaskRegistrar.removeCronTask(runnable);
        }

        // Add a new one
        if (Boolean.TRUE.equals(savedOne.getEnabled())) {
            TaskRunnable runnable = createTaskRunnable(savedOne);
            cronTaskRegistrar.addCronTask(runnable, domain.getCronExpression());
        }
    }

    @Override
    public void delete(String id) {
        RpcTask existingOne = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        taskRepository.deleteById(id);
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            TaskRunnable runnable = createTaskRunnable(existingOne);
            cronTaskRegistrar.removeCronTask(runnable);
        }
    }

    @Override
    public void startOrPause(String id) {
        RpcTask existingOne = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        TaskRunnable runnable = createTaskRunnable(existingOne);
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            cronTaskRegistrar.addCronTask(runnable, existingOne.getCronExpression());
        } else {
            cronTaskRegistrar.removeCronTask(runnable);
        }
    }

    private TaskRunnable createTaskRunnable(RpcTask task) {
        return TaskRunnable.builder()
                .taskHistoryRepository(taskHistoryRepository)
                .taskLockRepository(taskLockRepository)
                .rpcRegistryService(rpcRegistryService)
                .proxyFactory(Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory()))
                .name(task.getName())
                .registryIdentity(task.getRegistryIdentity())
                .interfaceName(task.getInterfaceName())
                .form(task.getForm())
                .version(task.getVersion())
                .methodName(task.getMethodName())
                .methodParamTypes(task.getMethodParamTypes())
                .argumentsJson(task.getArgumentsJson())
                .cronExpression(task.getCronExpression())
                .allHostsRun(task.isAllHostsRun())
                .build();
    }

    @Override
    public Page<RpcTask> find(Pageable pageable, String name, String interfaceName, String methodName) {
        RpcTask probe = new RpcTask();
        probe.setName(name);
        probe.setInterfaceName(interfaceName);
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        return taskRepository.findAll(Example.of(probe, matcher), pageable);
    }
}
