package org.infinity.luix.webcenter.listener;

import io.mongock.runner.spring.base.events.SpringMigrationSuccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.webcenter.service.RpcScheduledTaskService;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class MongockSuccessEventListener implements ApplicationListener<SpringMigrationSuccessEvent> {

    @Resource
    private RpcScheduledTaskService rpcScheduledTaskService;

    @Override
    public void onApplicationEvent(SpringMigrationSuccessEvent event) {
        log.info("Migrated mongock data");
        rpcScheduledTaskService.loadAll();
    }
}