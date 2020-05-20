package org.infinity.rpc.webcenter.service.impl;

import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.infinity.rpc.webcenter.domain.UserProfilePhoto;
import org.infinity.rpc.webcenter.repository.UserProfilePhotoRepository;
import org.infinity.rpc.webcenter.service.UserProfilePhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserProfilePhotoServiceImpl implements UserProfilePhotoService {

    @Autowired
    private UserProfilePhotoRepository userProfilePhotoRepository;

    @Override
    public UserProfilePhoto insert(String userName, byte[] photoData) {
        UserProfilePhoto photo = new UserProfilePhoto(userName);
        photo.setProfilePhoto(new Binary(BsonBinarySubType.BINARY, photoData));
        photo = userProfilePhotoRepository.insert(photo);
        return photo;
    }

    @Override
    public void update(String id, String userName, byte[] photoData) {
        UserProfilePhoto existingPhoto = userProfilePhotoRepository.findById(id).get();
        if (existingPhoto == null) {
            return;
        }
        existingPhoto.setUserName(userName);
        existingPhoto.setProfilePhoto(new Binary(BsonBinarySubType.BINARY, photoData));
        userProfilePhotoRepository.save(existingPhoto);
    }
}
