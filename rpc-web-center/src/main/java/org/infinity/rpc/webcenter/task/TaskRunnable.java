package org.infinity.rpc.webcenter.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.webcenter.domain.RpcTaskHistory;
import org.infinity.rpc.webcenter.domain.RpcTaskLock;
import org.infinity.rpc.webcenter.repository.RpcTaskHistoryRepository;
import org.infinity.rpc.webcenter.repository.RpcTaskLockRepository;
import org.infinity.rpc.webcenter.service.RpcRegistryService;
import org.infinity.rpc.webcenter.utils.NetworkUtils;
import org.springframework.util.StopWatch;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;
import static org.infinity.rpc.core.constant.ServiceConstants.FORM;
import static org.infinity.rpc.core.constant.ServiceConstants.VERSION;

@Slf4j
@EqualsAndHashCode
@Builder
public class TaskRunnable implements Runnable {

    private static final int SECOND = 1000;
    private static final int MINUTE = 60000;

    private final transient RpcTaskHistoryRepository taskHistoryRepository;
    private final transient RpcTaskLockRepository    taskLockRepository;
    private final transient RpcRegistryService       rpcRegistryService;
    private final transient Proxy                    proxyFactory;
    private final           String                   name;
    private final           String                   registryIdentity;
    private final           String                   interfaceName;
    private final           String                   form;
    private final           String                   version;
    private final           String                   methodName;
    private final           String[]                 methodParamTypes;
    private final           String                   methodSignature;
    private final           String                   argumentsJson;
    private final           String                   cronExpression;
    private final           boolean                  allHostsRun;

    @Override
    public void run() {
        if (Boolean.FALSE.equals(allHostsRun)) {
            // Single host execute mode
            if (taskLockRepository.findByName(name).isPresent()) {
                log.warn("Skip to execute task for the address: {}", NetworkUtils.INTRANET_IP);
                return;
            }
            // This distributed lock used to control that only one node executes the task at the same time
            RpcTaskLock taskLock = new RpcTaskLock();
            taskLock.setName(name);
            // Set expiry time with 30 seconds for the lock
            taskLock.setExpiryTime(Instant.now().plus(10, ChronoUnit.SECONDS));
            taskLockRepository.save(taskLock);
        }

        log.info("Executing timing task {}.{}({}) at {}", interfaceName, methodName, argumentsJson,
                ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        RpcTaskHistory taskHistory = new RpcTaskHistory();
        taskHistory.setName(name);
        taskHistory.setRegistryIdentity(registryIdentity);
        taskHistory.setInterfaceName(interfaceName);
        taskHistory.setForm(form);
        taskHistory.setVersion(version);
        taskHistory.setMethodName(methodName);
        taskHistory.setMethodParamTypes(methodParamTypes);
        taskHistory.setMethodSignature(methodSignature);
        taskHistory.setArgumentsJson(argumentsJson);
        taskHistory.setCronExpression(cronExpression);
        // Automatically delete records after 60 days
        taskHistory.setExpiryTime(Instant.now().plus(60, ChronoUnit.DAYS));

        try {
            // Select one of node to execute
            ConsumerStub<?> consumerStub = rpcRegistryService.getConsumerStub(registryIdentity,
                    null, interfaceName, ImmutableMap.of(FORM, form, VERSION, version));
            Object[] args = null;
            if (StringUtils.isNotEmpty(argumentsJson)) {
                args = new ObjectMapper().readValue(argumentsJson, Object[].class);
            }

            UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
            invocationHandler.invoke(methodName, methodParamTypes, args);
            taskHistory.setSuccess(true);
        } catch (Exception ex) {
            taskHistory.setSuccess(false);
            taskHistory.setReason(ex.getMessage());
            log.error(String.format("Failed to execute timing task %s.%s(%s)",
                    interfaceName, methodName, argumentsJson), ex);
        } finally {
            stopWatch.stop();
            long elapsed = stopWatch.getTotalTimeMillis();
            if (elapsed < SECOND) {
                log.info("Executed timing task {}.{}({}) with {}ms",
                        interfaceName, methodName, argumentsJson, elapsed);
            } else if (elapsed < MINUTE) {
                log.info("Executed timing task {}.{}({}) with {}s",
                        interfaceName, methodName, argumentsJson, elapsed / 1000);
            } else {
                log.info("Executed timing task {}.{}({}) with {}m",
                        interfaceName, methodName, argumentsJson, elapsed / (1000 * 60));
            }

            taskHistory.setElapsed(elapsed);
            // Save task history
            taskHistoryRepository.save(taskHistory);
        }
    }
}