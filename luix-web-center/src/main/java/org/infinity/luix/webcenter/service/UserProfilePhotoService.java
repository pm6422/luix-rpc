package org.infinity.luix.webcenter.service;

import org.infinity.luix.webcenter.domain.User;
import org.infinity.luix.webcenter.domain.UserProfilePhoto;

public interface UserProfilePhotoService {

    void insert(String userId, byte[] photoData);

    void update(UserProfilePhoto photo, byte[] photoData);

    void save(User user, byte[] photoData);
}