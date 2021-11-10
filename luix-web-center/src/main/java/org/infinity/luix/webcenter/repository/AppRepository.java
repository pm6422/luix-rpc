package org.infinity.luix.webcenter.repository;

import org.infinity.luix.webcenter.domain.App;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the App entity.
 */
@Repository
public interface AppRepository extends MongoRepository<App, String> {

}
