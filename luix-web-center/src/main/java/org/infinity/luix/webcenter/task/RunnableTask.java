package org.infinity.luix.webcenter.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.webcenter.domain.RpcScheduledTaskLock;
import org.infinity.luix.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.luix.core.client.proxy.Proxy;
import org.infinity.luix.core.client.stub.ConsumerStub;
import org.infinity.luix.webcenter.domain.RpcScheduledTask;
import org.infinity.luix.webcenter.domain.RpcScheduledTaskHistory;
import org.infinity.luix.webcenter.repository.RpcScheduledTaskHistoryRepository;
import org.infinity.luix.webcenter.repository.RpcScheduledTaskLockRepository;
import org.infinity.luix.webcenter.service.RpcRegistryService;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StopWatch;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;
import static org.infinity.luix.core.constant.ServiceConstants.*;

@Slf4j
@Builder
public class RunnableTask implements Runnable {

    private static final int SECOND = 1000;
    private static final int MINUTE = 60000;

    private final RpcScheduledTaskHistoryRepository rpcScheduledTaskHistoryRepository;
    private final RpcScheduledTaskLockRepository    rpcScheduledTaskLockRepository;
    private final RpcScheduledTask                  rpcScheduledTask;

    private final transient RpcRegistryService rpcRegistryService;
    private final transient Proxy              proxyFactory;
    private final           String             name;
    private final           String             registryIdentity;
    private final           String             interfaceName;
    private final           String             form;
    private final           String             version;
    private final           Integer            requestTimeout;
    private final           Integer            retryCount;

    @Override
    public void run() {
        Instant now = Instant.now();
        if (rpcScheduledTask.getStartTime() != null && now.isBefore(rpcScheduledTask.getStartTime())) {
            log.debug("It's not time to start yet for scheduled task: [{}]", rpcScheduledTask.getName());
            return;
        }
        if (rpcScheduledTask.getStopTime() != null && now.isAfter(rpcScheduledTask.getStopTime())) {
            log.debug("It's past the stop time for scheduled task: [{}]", rpcScheduledTask.getName());
            return;
        }
        // Single host execute mode
//        if (rpcScheduledTaskLockRepository.findByName(rpcScheduledTask.getName()).isPresent()) {
//            log.warn("Skip to execute scheduled task for the address: {}", NetworkUtils.INTRANET_IP);
//            return;
//        }
        // This distributed lock used to control that only one node executes the task at the same time
        RpcScheduledTaskLock scheduledTaskLock = new RpcScheduledTaskLock();
        scheduledTaskLock.setName(rpcScheduledTask.getName());
        // Set expiry time with 10 seconds for the lock
        scheduledTaskLock.setExpiryTime(Instant.now().plus(10, ChronoUnit.SECONDS));
        rpcScheduledTaskLockRepository.save(scheduledTaskLock);

        log.info("Executing scheduled task {}.{}({}) at {}", interfaceName, rpcScheduledTask.getMethodName(),
                rpcScheduledTask.getArgumentsJson(), ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        RpcScheduledTaskHistory scheduledTaskHistory = new RpcScheduledTaskHistory();
        BeanUtils.copyProperties(rpcScheduledTask, scheduledTaskHistory);
        scheduledTaskHistory.setId(null);

        // Automatically delete records after 60 days
        scheduledTaskHistory.setExpiryTime(Instant.now().plus(60, ChronoUnit.DAYS));

        try {
            // Execute task
            executeTask();
            scheduledTaskHistory.setSuccess(true);
        } catch (Exception ex) {
            scheduledTaskHistory.setSuccess(false);
            scheduledTaskHistory.setReason(ex.getMessage());
            log.error(String.format("Failed to execute timing task %s.%s(%s)", interfaceName,
                    rpcScheduledTask.getMethodName(), rpcScheduledTask.getArgumentsJson()), ex);
        } finally {
            stopWatch.stop();
            long elapsed = stopWatch.getTotalTimeMillis();
            if (elapsed < SECOND) {
                log.info("Executed scheduled task {}.{}({}) with {}ms",
                        interfaceName, rpcScheduledTask.getMethodName(), rpcScheduledTask.getArgumentsJson(), elapsed);
            } else if (elapsed < MINUTE) {
                log.info("Executed scheduled task {}.{}({}) with {}s",
                        interfaceName, rpcScheduledTask.getMethodName(), rpcScheduledTask.getArgumentsJson(), elapsed / 1000);
            } else {
                log.info("Executed scheduled task {}.{}({}) with {}m",
                        interfaceName, rpcScheduledTask.getMethodName(), rpcScheduledTask.getArgumentsJson(), elapsed / (1000 * 60));
            }

            scheduledTaskHistory.setElapsed(elapsed);
            // Save task history
            rpcScheduledTaskHistoryRepository.save(scheduledTaskHistory);
        }
    }

    private void executeTask() throws JsonProcessingException {
        // Select one of node to execute
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FORM, form);
        attributes.put(VERSION, version);
        if (requestTimeout != null) {
            attributes.put(REQUEST_TIMEOUT, requestTimeout.toString());
        }
        if (retryCount != null) {
            attributes.put(RETRY_COUNT, retryCount.toString());
        }

        ConsumerStub<?> consumerStub = rpcRegistryService.getConsumerStub(registryIdentity,
                null, interfaceName, attributes);
        Object[] args = null;
        if (StringUtils.isNotEmpty(rpcScheduledTask.getArgumentsJson())) {
            args = new ObjectMapper().readValue(rpcScheduledTask.getArgumentsJson(), Object[].class);
        }

        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        invocationHandler.invoke(rpcScheduledTask.getMethodName(), rpcScheduledTask.getMethodParamTypes(), args);
    }
}