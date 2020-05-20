package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.UserProfilePhoto;

public interface UserProfilePhotoService {

    UserProfilePhoto insert(String userName, byte[] photoData);

    void update(String id, String userName, byte[] photoData);
}