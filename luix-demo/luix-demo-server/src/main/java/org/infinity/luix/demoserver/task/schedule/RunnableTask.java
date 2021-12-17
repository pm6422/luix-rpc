package org.infinity.luix.demoserver.task.schedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.demoserver.domain.ScheduledTask;
import org.infinity.luix.demoserver.domain.ScheduledTaskHistory;
import org.infinity.luix.demoserver.domain.ScheduledTaskLock;
import org.infinity.luix.demoserver.repository.ScheduledTaskHistoryRepository;
import org.infinity.luix.demoserver.repository.ScheduledTaskLockRepository;
import org.infinity.luix.utilities.network.AddressUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
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

    private final ApplicationContext             applicationContext;
    private final ScheduledTaskHistoryRepository scheduledTaskHistoryRepository;
    private final ScheduledTaskLockRepository    scheduledTaskLockRepository;
    private final ScheduledTask                  scheduledTask;

    @Override
    public void run() {
        Instant now = Instant.now();
        if (scheduledTask.getStartTime() != null && now.isBefore(scheduledTask.getStartTime())) {
            log.debug("It's not time to start yet for scheduled task: [{}]", scheduledTask.getName());
            return;
        }
        if (scheduledTask.getStopTime() != null && now.isAfter(scheduledTask.getStopTime())) {
            log.debug("It's past the stop time for scheduled task: [{}]", scheduledTask.getName());
            return;
        }
        // Single host execute mode
        if (scheduledTaskLockRepository.findByName(scheduledTask.getName()).isPresent()) {
            log.warn("Skip to execute scheduled task for the address: {}", AddressUtils.getIntranetIp());
            return;
        }
        // This distributed lock used to control that only one node executes the task at the same time
        ScheduledTaskLock scheduledTaskLock = new ScheduledTaskLock();
        scheduledTaskLock.setName(scheduledTask.getName());
        // Set expiry time with 10 seconds for the lock
        scheduledTaskLock.setExpiryTime(Instant.now().plus(10, ChronoUnit.SECONDS));
        scheduledTaskLockRepository.save(scheduledTaskLock);

        log.info("Executing scheduled task {}.{}() at {}", scheduledTask.getBeanName(), TaskExecutable.METHOD_NAME,
                ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ScheduledTaskHistory scheduledTaskHistory = new ScheduledTaskHistory();
        BeanUtils.copyProperties(scheduledTask, scheduledTaskHistory);
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
            log.error(String.format("Failed to execute scheduled task %s.%s()", scheduledTask.getBeanName(), TaskExecutable.METHOD_NAME), ex);
        } finally {
            stopWatch.stop();
            long elapsed = stopWatch.getTotalTimeMillis();
            if (elapsed < SECOND) {
                log.info("Executed scheduled task {}.{}() in {}ms", scheduledTask.getBeanName(), TaskExecutable.METHOD_NAME, elapsed);
            } else if (elapsed < MINUTE) {
                log.info("Executed scheduled task {}.{}() in {}s", scheduledTask.getBeanName(), TaskExecutable.METHOD_NAME, elapsed / 1000);
            } else {
                log.warn("Executed scheduled task {}.{}() in {}m", scheduledTask.getBeanName(), TaskExecutable.METHOD_NAME, elapsed / (1000 * 60));
            }

            scheduledTaskHistory.setElapsed(elapsed);
            // Save task history
            scheduledTaskHistoryRepository.save(scheduledTaskHistory);
        }
    }

    private void executeTask() throws NoSuchMethodException, JsonProcessingException, IllegalAccessException, InvocationTargetException {
        Object target = applicationContext.getBean(scheduledTask.getBeanName());
        Method method = target.getClass().getDeclaredMethod(TaskExecutable.METHOD_NAME, Map.class);
        ReflectionUtils.makeAccessible(method);
        // Convert JSON string to Map
        Map<?, ?> arguments = new HashMap<>(16);
        if (StringUtils.isNotEmpty(scheduledTask.getArgumentsJson())) {
            arguments = new ObjectMapper().readValue(scheduledTask.getArgumentsJson(), Map.class);
        }
        method.invoke(target, arguments);
    }
}