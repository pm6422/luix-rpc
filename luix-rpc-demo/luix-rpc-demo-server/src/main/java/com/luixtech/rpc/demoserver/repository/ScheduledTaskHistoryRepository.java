package com.luixtech.rpc.demoserver.repository;

import com.luixtech.rpc.demoserver.domain.ScheduledTaskHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the ScheduledTaskHistory entity.
 */
@Repository
public interface ScheduledTaskHistoryRepository extends MongoRepository<ScheduledTaskHistory, String> {

}
