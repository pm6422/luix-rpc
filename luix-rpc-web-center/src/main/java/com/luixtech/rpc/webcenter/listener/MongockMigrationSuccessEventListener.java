package com.luixtech.rpc.webcenter.listener;

import com.luixtech.rpc.webcenter.service.RpcScheduledTaskService;
import io.mongock.runner.spring.base.events.SpringMigrationSuccessEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class MongockMigrationSuccessEventListener implements ApplicationListener<SpringMigrationSuccessEvent> {
    private final RpcScheduledTaskService rpcScheduledTaskService;

    @Override
    public void onApplicationEvent(SpringMigrationSuccessEvent event) {
        log.info("Migrated mongock data");
        rpcScheduledTaskService.loadAll();
    }
}