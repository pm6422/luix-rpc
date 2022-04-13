package com.luixtech.luixrpc.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.demoserver.task.schedule.TaskExecutable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("demoTaskService")
@Slf4j
public class DemoTaskServiceImpl implements TaskExecutable {
    @Override
    public void executeTask(Map<?, ?> arguments) {
        log.info("Perform scheduled task with arguments: {}", arguments);
    }
}
