package com.luixtech.rpc.demoserver.repository;

import com.luixtech.rpc.demoserver.domain.ScheduledTaskLock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the ScheduledTaskLock entity.
 */
@Repository
public interface ScheduledTaskLockRepository extends MongoRepository<ScheduledTaskLock, String> {

    Optional<ScheduledTaskLock> findByName(String name);

}
