package com.luixtech.rpc.webcenter.service;

import com.luixtech.rpc.webcenter.domain.User;
import com.luixtech.rpc.webcenter.domain.UserProfilePhoto;

public interface UserProfilePhotoService {

    void insert(String userId, byte[] photoData);

    void update(UserProfilePhoto photo, byte[] photoData);

    void save(User user, byte[] photoData);
}