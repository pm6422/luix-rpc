package org.infinity.luix.webcenter.repository;

import org.infinity.luix.webcenter.domain.Authority;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the Authority entity.
 */
@Repository
public interface AuthorityRepository extends MongoRepository<Authority, String> {

    List<Authority> findByEnabled(Boolean enabled);

}
