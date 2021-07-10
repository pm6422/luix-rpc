package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcTaskLock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the RpcTaskLock entity.
 */
@Repository
public interface RpcTaskLockRepository extends MongoRepository<RpcTaskLock, String> {

    Optional<RpcTaskLock> findByName(String name);

}
