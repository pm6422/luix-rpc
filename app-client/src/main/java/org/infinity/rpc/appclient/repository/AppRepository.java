package org.infinity.rpc.appclient.repository;

import org.infinity.rpc.appclient.domain.App;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the App entity.
 */
@Repository
public interface AppRepository extends MongoRepository<App, String> {

    List<App> findByEnabled(Boolean enabled);
}
