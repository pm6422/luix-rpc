package org.infinity.luix.webcenter.service;

import org.infinity.luix.webcenter.domain.RpcScheduledTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcScheduledTaskService {

    void loadAll();

    RpcScheduledTask insert(RpcScheduledTask domain);

    void update(RpcScheduledTask domain);

    void delete(String id);

    void startOrStop(String id);

    Page<RpcScheduledTask> find(Pageable pageable, String registryIdentity, String name, String interfaceName,
                                String form, String version, String methodName, String methodSignature);
}
