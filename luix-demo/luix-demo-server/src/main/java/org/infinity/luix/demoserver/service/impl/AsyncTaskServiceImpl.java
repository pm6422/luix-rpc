package org.infinity.luix.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.demoserver.service.AsyncTaskService;
import org.infinity.luix.demoserver.task.polling.queue.Message;
import org.infinity.luix.demoserver.task.polling.queue.MessageQueue;
import org.infinity.luix.demoserver.utils.TraceIdUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

    @Override
    public String sendMessage() {
        try {
            TimeUnit.SECONDS.sleep(5);
            return "Task " + TraceIdUtils.getTraceId() + " finished";
        } catch (InterruptedException e) {
            throw new RuntimeException();
        }
    }

    @Override
    @Async
    public void sendMessage(Message message) {
        try {
            log.info("Sending message {}", message);
            TimeUnit.SECONDS.sleep(5);
            MessageQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
