package com.luixtech.luixrpc.webcenter.service;

import com.luixtech.luixrpc.webcenter.domain.User;
import com.luixtech.luixrpc.webcenter.domain.UserProfilePhoto;

public interface UserProfilePhotoService {

    void insert(String userId, byte[] photoData);

    void update(UserProfilePhoto photo, byte[] photoData);

    void save(User user, byte[] photoData);
}