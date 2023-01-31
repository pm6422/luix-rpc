package com.luixtech.rpc.demoserver.service.impl;

import com.luixtech.rpc.demoserver.task.schedule.TaskExecutable;
import lombok.extern.slf4j.Slf4j;
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
