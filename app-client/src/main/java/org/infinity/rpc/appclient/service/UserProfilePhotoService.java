package org.infinity.rpc.appclient.service;

import org.infinity.rpc.appclient.domain.UserProfilePhoto;

public interface UserProfilePhotoService {

    UserProfilePhoto insert(String userName, byte[] photoData);

    void update(String id, String userName, byte[] photoData);
}