package org.infinity.rpc.demoserver.service.impl;

import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.infinity.rpc.core.server.annotation.RpcProvider;
import org.infinity.rpc.democommon.domain.User;
import org.infinity.rpc.democommon.domain.UserProfilePhoto;
import org.infinity.rpc.democommon.service.UserProfilePhotoService;
import org.infinity.rpc.demoserver.repository.UserProfilePhotoRepository;
import org.infinity.rpc.demoserver.repository.UserRepository;

import java.util.Optional;

@RpcProvider
public class UserProfilePhotoServiceImpl implements UserProfilePhotoService {

    private final UserProfilePhotoRepository userProfilePhotoRepository;
    private final UserRepository             userRepository;

    public UserProfilePhotoServiceImpl(UserProfilePhotoRepository userProfilePhotoRepository,
                                       UserRepository userRepository) {
        this.userProfilePhotoRepository = userProfilePhotoRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void insert(String userId, byte[] photoData) {
        UserProfilePhoto photo = new UserProfilePhoto(userId, new Binary(BsonBinarySubType.BINARY, photoData));
        userProfilePhotoRepository.insert(photo);
    }

    @Override
    public void update(UserProfilePhoto photo, byte[] photoData) {
        photo.setProfilePhoto(new Binary(BsonBinarySubType.BINARY, photoData));
        userProfilePhotoRepository.save(photo);
    }

    @Override
    public void save(User user, byte[] photoData) {
        Optional<UserProfilePhoto> existingPhoto = userProfilePhotoRepository.findByUserId(user.getId());
        if (existingPhoto.isPresent()) {
            // Update if exists
            update(existingPhoto.get(), photoData);
        } else {
            // Insert if not exists
            insert(user.getId(), photoData);
            // Update hasProfilePhoto to true
            user.setHasProfilePhoto(true);
            userRepository.save(user);
        }
    }
}
