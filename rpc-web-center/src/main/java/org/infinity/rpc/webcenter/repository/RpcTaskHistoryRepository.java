package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcScheduledTaskHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the RpcTaskHistory entity.
 */
@Repository
public interface RpcTaskHistoryRepository extends MongoRepository<RpcScheduledTaskHistory, String> {

}
