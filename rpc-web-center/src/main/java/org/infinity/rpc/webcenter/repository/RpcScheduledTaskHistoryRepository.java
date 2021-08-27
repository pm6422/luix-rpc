package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcScheduledTaskHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the RpcScheduledTaskHistory entity.
 */
@Repository
public interface RpcScheduledTaskHistoryRepository extends MongoRepository<RpcScheduledTaskHistory, String> {

}
