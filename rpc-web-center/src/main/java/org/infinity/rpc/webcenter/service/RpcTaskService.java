package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.RpcTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcTaskService {

    void refresh() throws Exception;

    RpcTask insert(RpcTask domain);

    void update(RpcTask domain);

    void delete(String id);

    void startOrPause(String id);

    Page<RpcTask> find(Pageable pageable, String name, String interfaceName, String methodName);
}
