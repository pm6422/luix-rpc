package org.infinity.rpc.demoserver.service;

import org.infinity.rpc.demoserver.domain.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    void refresh() throws Exception;

    Task insert(Task domain);

    void update(Task domain);

    void delete(String id);

    void startOrStop(String id);

    Page<Task> find(Pageable pageable, String name, String beanName);
}
