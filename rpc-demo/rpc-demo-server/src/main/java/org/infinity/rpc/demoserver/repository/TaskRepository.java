package org.infinity.rpc.demoserver.repository;

import org.infinity.rpc.demoserver.domain.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the Task entity.
 */
@Repository
public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByEnabledIsTrue();
}
