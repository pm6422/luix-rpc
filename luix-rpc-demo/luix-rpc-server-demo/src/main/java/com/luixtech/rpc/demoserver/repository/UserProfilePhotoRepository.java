package com.luixtech.rpc.demoserver.repository;

import com.luixtech.rpc.democommon.domain.UserProfilePhoto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the UserProfilePhoto entity.
 */
@Repository
public interface UserProfilePhotoRepository extends MongoRepository<UserProfilePhoto, String> {

    Optional<UserProfilePhoto> findByUserId(String userId);
}
