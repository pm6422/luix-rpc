package com.luixtech.rpc.democommon.service;

import com.luixtech.rpc.democommon.domain.User;
import com.luixtech.rpc.democommon.domain.UserProfilePhoto;

public interface UserProfilePhotoService {

    void insert(String userId, byte[] photoData);

    void update(UserProfilePhoto photo, byte[] photoData);

    void save(User user, byte[] photoData);
}