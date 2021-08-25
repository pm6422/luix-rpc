package org.infinity.rpc.demoserver.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.demoserver.RpcDemoServerLauncher;
import org.infinity.rpc.demoserver.domain.TaskHistory;
import org.infinity.rpc.demoserver.domain.TaskLock;
import org.infinity.rpc.demoserver.repository.TaskHistoryRepository;
import org.infinity.rpc.demoserver.repository.TaskLockRepository;
import org.infinity.rpc.demoserver.utils.NetworkUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StopWatch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;

@Slf4j
@Builder
public class RunnableTask implements Runnable {

    private static final int SECOND = 1000;
    private static final int MINUTE = 60000;

    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskLockRepository    taskLockRepository;
    private final String                name;
    private final String                beanName;
    private final String                argumentsJson;
    private final String                cronExpression;

    @Override
    public void run() {
        // Single host execute mode
        if (taskLockRepository.findByName(name).isPresent()) {
            log.warn("Skip to execute task for the address: {}", NetworkUtils.INTRANET_IP);
            return;
        }
        // This distributed lock used to control that only one node executes the task at the same time
        TaskLock taskLock = new TaskLock();
        taskLock.setName(name);
        // Set expiry time with 30 seconds for the lock
        taskLock.setExpiryTime(Instant.now().plus(30, ChronoUnit.SECONDS));
        taskLockRepository.save(taskLock);

        log.info("Executing timing task {}.{}() at {}", beanName, TaskExecutable.METHOD_NAME,
                ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        TaskHistory taskHistory = new TaskHistory();
        taskHistory.setName(name);
        taskHistory.setBeanName(beanName);
        taskHistory.setArgumentsJson(argumentsJson);
        taskHistory.setCronExpression(cronExpression);
        // Automatically delete records after 60 days
        taskHistory.setExpiryTime(Instant.now().plus(60, ChronoUnit.DAYS));

        try {
            // Execute task
            executeTask();
            taskHistory.setSuccess(true);
        } catch (Exception ex) {
            taskHistory.setSuccess(false);
            taskHistory.setReason(ex.getMessage());
            log.error(String.format("Failed to execute timing task %s.%s()", beanName, TaskExecutable.METHOD_NAME), ex);
        } finally {
            stopWatch.stop();
            long elapsed = stopWatch.getTotalTimeMillis();
            if (elapsed < SECOND) {
                log.info("Executed timing task {}.{}() in {}ms", beanName, TaskExecutable.METHOD_NAME, elapsed);
            } else if (elapsed < MINUTE) {
                log.info("Executed timing task {}.{}() in {}s", beanName, TaskExecutable.METHOD_NAME, elapsed / 1000);
            } else {
                log.warn("Executed timing task {}.{}() in {}m", beanName, TaskExecutable.METHOD_NAME, elapsed / (1000 * 60));
            }

            taskHistory.setElapsed(elapsed);
            // Save task history
            taskHistoryRepository.save(taskHistory);
        }
    }

    private void executeTask() throws NoSuchMethodException, JsonProcessingException, IllegalAccessException, InvocationTargetException {
        Object target = RpcDemoServerLauncher.applicationContext.getBean(beanName);
        Method method = target.getClass().getDeclaredMethod(TaskExecutable.METHOD_NAME, Map.class);
        ReflectionUtils.makeAccessible(method);
        // Convert JSON string to Map
        Map<?, ?> arguments = new HashMap<>(16);
        if (StringUtils.isNotEmpty(argumentsJson)) {
            arguments = new ObjectMapper().readValue(argumentsJson, Map.class);
        }
        method.invoke(target, arguments);
    }
}