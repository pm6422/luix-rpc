package org.infinity.rpc.demoserver.service;

import org.infinity.rpc.demoserver.domain.Task;

public interface TaskService {

    void refresh() throws Exception;

    Task insert(Task domain);

    void update(Task domain);

    void delete(String id);

    void startOrPause(String id);
}
