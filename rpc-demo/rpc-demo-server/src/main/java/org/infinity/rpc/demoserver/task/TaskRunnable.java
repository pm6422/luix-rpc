package org.infinity.rpc.demoserver.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.demoserver.RpcDemoServerLauncher;
import org.infinity.rpc.demoserver.domain.Task;
import org.infinity.rpc.demoserver.domain.TaskHistory;
import org.infinity.rpc.demoserver.repository.TaskHistoryRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;

@Slf4j
@EqualsAndHashCode
@Builder
public class TaskRunnable implements Runnable {

    private static final int                   SECOND = 1000;
    private static final int                   MINUTE = 60000;
    private final        TaskHistoryRepository taskHistoryRepository;
    private final        Task                  task;

    @Override
    public void run() {
        log.info("Executing timing task {}.{}({}) at {}", task.getBeanName(), Taskable.METHOD_NAME, task.getArgumentsJson(),
                ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        TaskHistory taskHistory = new TaskHistory();
        BeanUtils.copyProperties(task, taskHistory);
        // Automatically delete records after 60 days
        taskHistory.setExpiryTime(Instant.now().plus(60, ChronoUnit.DAYS));
        taskHistory.setId(null);
        taskHistory.setCreatedTime(null);

        try {
            Object target = RpcDemoServerLauncher.applicationContext.getBean(task.getBeanName());
            Method method = target.getClass().getDeclaredMethod(Taskable.METHOD_NAME, Map.class);
            ReflectionUtils.makeAccessible(method);
            // Convert JSON string to Map
            Map<?, ?> arguments = new HashMap<>(16);
            if (StringUtils.isNotEmpty(task.getArgumentsJson())) {
                arguments = new ObjectMapper().readValue(task.getArgumentsJson(), Map.class);
            }
            method.invoke(target, arguments);
            taskHistory.setSuccess(true);
        } catch (Exception ex) {
            taskHistory.setSuccess(false);
            taskHistory.setReason(ex.getMessage());
            log.error(String.format("Failed to execute timing task %s.%s(%s)",
                    task.getBeanName(), Taskable.METHOD_NAME, task.getArgumentsJson()), ex);
        } finally {
            stopWatch.stop();
            long elapsed = stopWatch.getTotalTimeMillis();
            if (elapsed < SECOND) {
                log.info("Executed timing task {}.{}({}) with {}ms",
                        task.getBeanName(), Taskable.METHOD_NAME, task.getArgumentsJson(), elapsed);
            } else if (elapsed < MINUTE) {
                log.info("Executed timing task {}.{}({}) with {}s",
                        task.getBeanName(), Taskable.METHOD_NAME, task.getArgumentsJson(), elapsed / 1000);
            } else {
                log.info("Executed timing task {}.{}({}) with {}m",
                        task.getBeanName(), Taskable.METHOD_NAME, task.getArgumentsJson(), elapsed / (1000 * 60));
            }

            taskHistory.setElapsed(elapsed);
            // Save task history
            taskHistoryRepository.save(taskHistory);
        }
    }
}