package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.User;
import org.infinity.rpc.webcenter.domain.UserProfilePhoto;

public interface UserProfilePhotoService {

    void insert(String userId, byte[] photoData);

    void update(UserProfilePhoto photo, byte[] photoData);

    void save(User user, byte[] photoData);
}