package com.luixtech.rpc.democommon.service;

import com.luixtech.rpc.democommon.domain.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    User findOneByLogin(String login);

    User findOneByMobileNo(Integer mobileNo);

    User findOneByMobile(int mobileNo);

    List<User> findByWeight(Double weight);

    List<User> findByEnabled(Pageable page, Boolean enabled);

    List<User> findByEnabledIsTrue();

    boolean save(User user);
}