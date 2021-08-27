package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.RpcScheduledTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcTaskService {

    void refresh() throws Exception;

    RpcScheduledTask insert(RpcScheduledTask domain);

    void update(RpcScheduledTask domain);

    void delete(String id);

    void startOrPause(String id);

    Page<RpcScheduledTask> find(Pageable pageable, String registryIdentity, String name, String interfaceName,
                                String form, String version, String methodName, String methodSignature);
}
