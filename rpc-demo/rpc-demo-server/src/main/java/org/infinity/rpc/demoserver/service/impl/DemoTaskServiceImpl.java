package org.infinity.rpc.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("demoTaskService")
@Slf4j
public class DemoTaskServiceImpl {
    public void taskWithParams(String params) {
        log.info("Perform timing task with params:" + params);
    }

    public void taskNoParams() {
        log.info("Perform timing task");
    }
}
