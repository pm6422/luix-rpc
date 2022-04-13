package com.luixtech.rpc.webcenter.repository;

import com.luixtech.rpc.webcenter.domain.RpcScheduledTask;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the RpcScheduledTask entity.
 */
@Repository
public interface RpcScheduledTaskRepository extends MongoRepository<RpcScheduledTask, String> {

    List<RpcScheduledTask> findByEnabledIsTrue();
}
