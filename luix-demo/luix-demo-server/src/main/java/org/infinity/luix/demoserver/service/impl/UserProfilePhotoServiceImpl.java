package org.infinity.luix.demoserver.service.impl;

import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.infinity.luix.core.server.annotation.RpcProvider;
import org.infinity.luix.democommon.domain.User;
import org.infinity.luix.democommon.domain.UserProfilePhoto;
import org.infinity.luix.democommon.service.UserProfilePhotoService;
import org.infinity.luix.demoserver.repository.UserProfilePhotoRepository;
import org.infinity.luix.demoserver.repository.UserRepository;

import javax.annotation.Resource;
import java.util.Optional;

@RpcProvider
public class UserProfilePhotoServiceImpl implements UserProfilePhotoService {

    @Resource
    private UserProfilePhotoRepository userProfilePhotoRepository;
    @Resource
    private UserRepository             userRepository;

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
