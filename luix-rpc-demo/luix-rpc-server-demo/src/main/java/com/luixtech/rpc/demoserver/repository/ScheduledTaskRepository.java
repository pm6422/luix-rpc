package com.luixtech.rpc.demoserver.repository;

import com.luixtech.rpc.demoserver.domain.ScheduledTask;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the ScheduledTask entity.
 */
@Repository
public interface ScheduledTaskRepository extends MongoRepository<ScheduledTask, String> {

    List<ScheduledTask> findByEnabledIsTrue();
}
