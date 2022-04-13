package com.luixtech.luixrpc.democommon.service;

import com.luixtech.luixrpc.democommon.domain.User;
import com.luixtech.luixrpc.democommon.domain.UserProfilePhoto;

public interface UserProfilePhotoService {

    void insert(String userId, byte[] photoData);

    void update(UserProfilePhoto photo, byte[] photoData);

    void save(User user, byte[] photoData);
}