package org.infinity.luix.webcenter.repository;

import org.infinity.luix.webcenter.domain.UserAuthority;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the UserAuthority entity.
 */
@Repository
public interface UserAuthorityRepository extends MongoRepository<UserAuthority, String> {

    List<UserAuthority> findByUserId(String userId);

    void deleteByUserId(String userId);

}
