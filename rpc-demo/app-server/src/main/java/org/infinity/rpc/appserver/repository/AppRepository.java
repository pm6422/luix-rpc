package org.infinity.rpc.appserver.repository;

import org.infinity.app.common.domain.App;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the App entity.
 */
@Repository
public interface AppRepository extends MongoRepository<App, String> {

}
