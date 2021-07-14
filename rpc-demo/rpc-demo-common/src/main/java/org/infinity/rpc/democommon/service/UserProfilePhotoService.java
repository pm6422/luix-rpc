package org.infinity.rpc.democommon.service;

import org.infinity.rpc.democommon.domain.User;
import org.infinity.rpc.democommon.domain.UserProfilePhoto;

public interface UserProfilePhotoService {

    void insert(String userId, byte[] photoData);

    void update(UserProfilePhoto photo, byte[] photoData);

    void save(User user, byte[] photoData);
}