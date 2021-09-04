package org.infinity.luix.democommon.service;

import org.infinity.luix.democommon.domain.User;
import org.infinity.luix.democommon.domain.UserProfilePhoto;

public interface UserProfilePhotoService {

    void insert(String userId, byte[] photoData);

    void update(UserProfilePhoto photo, byte[] photoData);

    void save(User user, byte[] photoData);
}