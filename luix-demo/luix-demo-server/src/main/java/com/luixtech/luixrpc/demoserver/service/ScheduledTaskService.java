package com.luixtech.luixrpc.demoserver.service;

import com.luixtech.luixrpc.demoserver.domain.ScheduledTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScheduledTaskService {

    ScheduledTask insert(ScheduledTask domain);

    void update(ScheduledTask domain);

    void delete(String id);

    void startOrStop(String id);

    Page<ScheduledTask> find(Pageable pageable, String name, String beanName);
}
