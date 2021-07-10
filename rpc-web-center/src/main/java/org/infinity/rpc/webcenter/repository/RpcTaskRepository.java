package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcTask;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the RpcTask entity.
 */
@Repository
public interface RpcTaskRepository extends MongoRepository<RpcTask, String> {

    List<RpcTask> findByEnabledIsTrue();
}
