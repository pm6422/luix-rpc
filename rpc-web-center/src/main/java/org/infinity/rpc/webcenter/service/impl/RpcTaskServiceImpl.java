package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Pattern;

import static org.infinity.rpc.webcenter.domain.RpcTask.*;

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
    @Resource
    private MongoTemplate            mongoTemplate;

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
    public Page<RpcTask> find(Pageable pageable, String registryIdentity, String name, String interfaceName,
                              String form, String version, String methodName, String methodSignature) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(name)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_NAME).regex(pattern));
        }
        if (StringUtils.isNotEmpty(interfaceName)) {
            query.addCriteria(Criteria.where(FIELD_INTERFACE_NAME).is(interfaceName));
        }
        if (StringUtils.isNotEmpty(form)) {
            query.addCriteria(Criteria.where(FIELD_FORM).is(form));
        }
        if (StringUtils.isNotEmpty(version)) {
            query.addCriteria(Criteria.where(FIELD_VERSION).is(version));
        }
        if (StringUtils.isNotEmpty(methodName)) {
            query.addCriteria(Criteria.where(FIELD_METHOD_NAME).is(methodName));
        }
        if (StringUtils.isNotEmpty(methodSignature)) {
            query.addCriteria(Criteria.where(FIELD_METHOD_SIGNATURE).is(methodSignature));
        }
        long totalCount = mongoTemplate.count(query, RpcTask.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcTask.class), pageable, totalCount);
    }
}
