package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.core.client.proxy.Proxy;
import com.luixtech.rpc.spring.boot.starter.config.LuixRpcProperties;
import com.luixtech.rpc.webcenter.domain.RpcScheduledTask;
import com.luixtech.rpc.webcenter.exception.DataNotFoundException;
import com.luixtech.rpc.webcenter.repository.RpcScheduledTaskHistoryRepository;
import com.luixtech.rpc.webcenter.repository.RpcScheduledTaskLockRepository;
import com.luixtech.rpc.webcenter.repository.RpcScheduledTaskRepository;
import com.luixtech.rpc.webcenter.service.RpcRegistryService;
import com.luixtech.rpc.webcenter.service.RpcScheduledTaskService;
import com.luixtech.rpc.webcenter.task.schedule.CancelableScheduledTaskRegistrar;
import com.luixtech.rpc.webcenter.task.schedule.RunnableTask;
import com.luixtech.uidgenerator.core.id.IdGenerator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
@Slf4j
public class RpcScheduledTaskServiceImpl implements RpcScheduledTaskService {
    private final RpcScheduledTaskRepository        rpcScheduledTaskRepository;
    private final RpcScheduledTaskHistoryRepository rpcScheduledTaskHistoryRepository;
    private final RpcScheduledTaskLockRepository    rpcScheduledTaskLockRepository;
    private final RpcRegistryService                rpcRegistryService;
    private final CancelableScheduledTaskRegistrar cancelableScheduledTaskRegistrar;
    private final LuixRpcProperties                luixRpcProperties;
    private final MongoTemplate                    mongoTemplate;

    @Override
    public void loadAll() {
        // Timed task with normal state in initial load database
        List<RpcScheduledTask> enabledScheduledTasks = rpcScheduledTaskRepository.findByEnabledIsTrue();
        if (CollectionUtils.isEmpty(enabledScheduledTasks)) {
            log.info("No scheduled tasks to execute!");
            return;
        }

        enabledScheduledTasks.forEach(this::addTask);
        log.info("Loaded all scheduled tasks");
    }

    @Override
    public RpcScheduledTask insert(RpcScheduledTask domain) {
        domain.setName("T" + IdGenerator.generateShortId());
        RpcScheduledTask savedOne = rpcScheduledTaskRepository.insert(domain);
        if (Boolean.TRUE.equals(savedOne.getEnabled())) {
            addTask(savedOne);
        }
        return savedOne;
    }

    @Override
    public void update(RpcScheduledTask domain) {
        RpcScheduledTask existingOne = rpcScheduledTaskRepository.findById(domain.getId()).orElseThrow(() -> new DataNotFoundException(domain.getId()));
        if (Boolean.TRUE.equals(domain.getUseCronExpression())) {
            domain.setFixedInterval(null);
            domain.setFixedIntervalUnit(null);
        } else {
            domain.setCronExpression(null);
        }

        RpcScheduledTask savedOne = rpcScheduledTaskRepository.save(domain);

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
        RpcScheduledTask existingOne = rpcScheduledTaskRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        rpcScheduledTaskRepository.deleteById(id);
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            removeTask(existingOne);
        }
    }

    @Override
    public void startOrStop(String id) {
        RpcScheduledTask existingOne = rpcScheduledTaskRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        if (Boolean.TRUE.equals(existingOne.getEnabled())) {
            addTask(existingOne);
        } else {
            removeTask(existingOne);
        }
    }

    @Override
    public Page<RpcScheduledTask> find(Pageable pageable, String registryIdentity, String name, String interfaceName,
                                       String form, String version, String methodName, String methodSignature) {
        Query query = Query.query(Criteria.where(RpcScheduledTask.FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(name)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(RpcScheduledTask.FIELD_NAME).regex(pattern));
        }
        if (StringUtils.isNotEmpty(interfaceName)) {
            query.addCriteria(Criteria.where(RpcScheduledTask.FIELD_INTERFACE_NAME).is(interfaceName));
        }
        if (StringUtils.isNotEmpty(form)) {
            query.addCriteria(Criteria.where(RpcScheduledTask.FIELD_FORM).is(form));
        }
        if (StringUtils.isNotEmpty(version)) {
            query.addCriteria(Criteria.where(RpcScheduledTask.FIELD_VERSION).is(version));
        }
        if (StringUtils.isNotEmpty(methodName)) {
            query.addCriteria(Criteria.where(RpcScheduledTask.FIELD_METHOD_NAME).is(methodName));
        }
        if (StringUtils.isNotEmpty(methodSignature)) {
            query.addCriteria(Criteria.where(RpcScheduledTask.FIELD_METHOD_SIGNATURE).is(methodSignature));
        }
        long totalCount = mongoTemplate.count(query, RpcScheduledTask.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcScheduledTask.class), pageable, totalCount);
    }

    private void addTask(RpcScheduledTask scheduledTask) {
        RunnableTask runnableTask = RunnableTask.builder()
                .rpcScheduledTaskHistoryRepository(rpcScheduledTaskHistoryRepository)
                .rpcScheduledTaskLockRepository(rpcScheduledTaskLockRepository)
                .rpcScheduledTask(scheduledTask)
                .rpcRegistryService(rpcRegistryService)
                .proxyFactory(Proxy.getInstance(luixRpcProperties.getConsumer().getProxyFactory()))
                .name(scheduledTask.getName())
                .registryIdentity(scheduledTask.getRegistryIdentity())
                .interfaceName(scheduledTask.getInterfaceName())
                .form(scheduledTask.getForm())
                .version(scheduledTask.getVersion())
                .requestTimeout(scheduledTask.getRequestTimeout())
                .retryCount(scheduledTask.getRetryCount())
                .faultTolerance(scheduledTask.getFaultTolerance())
                .build();
        if (Boolean.TRUE.equals(scheduledTask.getUseCronExpression())) {
            cancelableScheduledTaskRegistrar.addCronTask(scheduledTask.getName(), runnableTask, scheduledTask.getCronExpression());
        } else {
            cancelableScheduledTaskRegistrar.addFixedRateTask(scheduledTask.getName(), runnableTask, calculateMilliSeconds(scheduledTask));
        }
    }

    private void removeTask(RpcScheduledTask scheduledTask) {
        cancelableScheduledTaskRegistrar.removeTask(scheduledTask.getName());
    }

    private long calculateMilliSeconds(RpcScheduledTask scheduledTask) {
        long oneSecond = 1_000;
        if (RpcScheduledTask.UNIT_SECONDS.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneSecond * scheduledTask.getFixedInterval();
        } else if (RpcScheduledTask.UNIT_MINUTES.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneSecond * 60 * scheduledTask.getFixedInterval();
        } else if (RpcScheduledTask.UNIT_HOURS.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneSecond * 60 * 60 * scheduledTask.getFixedInterval();
        } else if (RpcScheduledTask.UNIT_DAYS.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneSecond * 60 * 60 * 24 * scheduledTask.getFixedInterval();
        }
        throw new IllegalStateException("Illegal fixed interval time unit!");
    }
}
