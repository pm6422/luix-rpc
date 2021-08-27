package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcScheduledTaskLock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the RpcTaskLock entity.
 */
@Repository
public interface RpcTaskLockRepository extends MongoRepository<RpcScheduledTaskLock, String> {

    Optional<RpcScheduledTaskLock> findByName(String name);

}
