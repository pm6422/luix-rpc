package org.infinity.rpc.appclient.repository;

import org.infinity.rpc.appclient.domain.UserAuthority;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the UserAuthority entity.
 */
@Repository
public interface UserAuthorityRepository extends MongoRepository<UserAuthority, String> {

    List<UserAuthority> findByUserId(String userId);

    Optional<UserAuthority> findOneByUserIdAndAuthorityName(String userId, String authorityName);

    void deleteByUserId(String userId);

}
