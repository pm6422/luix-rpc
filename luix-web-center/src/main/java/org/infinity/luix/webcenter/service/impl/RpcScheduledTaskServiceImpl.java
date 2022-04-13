package org.infinity.luix.webcenter.service.impl;

import com.luixtech.uidgenerator.core.id.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.client.proxy.Proxy;
import org.infinity.luix.spring.boot.config.LuixProperties;
import org.infinity.luix.webcenter.domain.RpcScheduledTask;
import org.infinity.luix.webcenter.exception.DataNotFoundException;
import org.infinity.luix.webcenter.repository.RpcScheduledTaskHistoryRepository;
import org.infinity.luix.webcenter.repository.RpcScheduledTaskLockRepository;
import org.infinity.luix.webcenter.repository.RpcScheduledTaskRepository;
import org.infinity.luix.webcenter.service.RpcRegistryService;
import org.infinity.luix.webcenter.service.RpcScheduledTaskService;
import org.infinity.luix.webcenter.task.schedule.CancelableScheduledTaskRegistrar;
import org.infinity.luix.webcenter.task.schedule.RunnableTask;
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

import static org.infinity.luix.webcenter.domain.RpcScheduledTask.*;

@Service
@Slf4j
public class RpcScheduledTaskServiceImpl implements RpcScheduledTaskService {
    @Resource
    private RpcScheduledTaskRepository        rpcScheduledTaskRepository;
    @Resource
    private RpcScheduledTaskHistoryRepository rpcScheduledTaskHistoryRepository;
    @Resource
    private RpcScheduledTaskLockRepository    rpcScheduledTaskLockRepository;
    @Resource
    private RpcRegistryService                rpcRegistryService;
    @Resource
    private CancelableScheduledTaskRegistrar  cancelableScheduledTaskRegistrar;
    @Resource
    private LuixProperties                    luixProperties;
    @Resource
    private MongoTemplate                     mongoTemplate;

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
                .proxyFactory(Proxy.getInstance(luixProperties.getConsumer().getProxyFactory()))
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
        if (UNIT_SECONDS.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneSecond * scheduledTask.getFixedInterval();
        } else if (UNIT_MINUTES.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneSecond * 60 * scheduledTask.getFixedInterval();
        } else if (UNIT_HOURS.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneSecond * 60 * 60 * scheduledTask.getFixedInterval();
        } else if (UNIT_DAYS.equals(scheduledTask.getFixedIntervalUnit())) {
            return oneSecond * 60 * 60 * 24 * scheduledTask.getFixedInterval();
        }
        throw new IllegalStateException("Illegal fixed interval time unit!");
    }
}
