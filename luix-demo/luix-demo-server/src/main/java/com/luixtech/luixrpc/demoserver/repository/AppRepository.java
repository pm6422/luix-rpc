package com.luixtech.luixrpc.demoserver.repository;

import com.luixtech.luixrpc.democommon.domain.App;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the App entity.
 */
@Repository
public interface AppRepository extends MongoRepository<App, String> {

}
