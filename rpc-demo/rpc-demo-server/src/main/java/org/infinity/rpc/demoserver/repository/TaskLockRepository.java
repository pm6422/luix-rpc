package org.infinity.rpc.demoserver.repository;

import org.infinity.rpc.demoserver.domain.TaskLock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the TaskLock entity.
 */
@Repository
public interface TaskLockRepository extends MongoRepository<TaskLock, String> {

    Optional<TaskLock> findByName(String name);

}
