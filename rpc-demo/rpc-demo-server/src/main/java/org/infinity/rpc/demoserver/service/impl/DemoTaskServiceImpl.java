package org.infinity.rpc.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.demoserver.task.TaskExecutable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("demoTaskService")
@Slf4j
public class DemoTaskServiceImpl implements TaskExecutable {
    @Override
    public void executeTask(Map<?, ?> arguments) {
        log.info("Perform timing task with arguments: {}", arguments);
    }
}
