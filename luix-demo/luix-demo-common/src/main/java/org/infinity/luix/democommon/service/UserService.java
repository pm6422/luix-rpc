package org.infinity.luix.democommon.service;

import org.infinity.luix.democommon.domain.User;

import java.util.List;

public interface UserService {

    User findOneByLogin(String login);

    User findOneByMobileNo(Integer mobileNo);

    User findOneByMobile(int mobileNo);

    List<User> findByWeight(Double weight);

    List<User> findByEnabled(Boolean enabled);

    boolean save(User user);
}