package org.infinity.rpc.demoserver.repository;

import org.infinity.rpc.demoserver.domain.TaskHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the TaskHistory entity.
 */
@Repository
public interface TaskHistoryRepository extends MongoRepository<TaskHistory, String> {

}
