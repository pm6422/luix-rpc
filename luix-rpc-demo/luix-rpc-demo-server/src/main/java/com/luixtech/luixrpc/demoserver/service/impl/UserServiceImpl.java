package com.luixtech.luixrpc.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.server.annotation.RpcProvider;
import com.luixtech.luixrpc.democommon.domain.User;
import com.luixtech.luixrpc.democommon.service.UserService;
import com.luixtech.luixrpc.demoserver.repository.UserRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@RpcProvider
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserRepository userRepository;

    @Override
    public User findOneByLogin(String login) {
        Assert.hasText(login, "it must not be null, empty, or blank");
        return userRepository.findOneByUserNameOrEmailOrMobileNo(login.toLowerCase(Locale.ENGLISH),
                login.toLowerCase(Locale.ENGLISH), login.toLowerCase(Locale.ENGLISH)).orElse(null);
    }

    @Override
    public User findOneByMobileNo(Integer mobileNo) {
        return findOneByMobile(mobileNo);
    }

    @Override
    public User findOneByMobile(int mobileNo) {
        User user = new User();
        user.setUserName("dummy");
        user.setMobileNo(String.valueOf(mobileNo));
        return user;
    }

    @Override
    public List<User> findByWeight(Double weight) {
        User user = new User();
        user.setUserName("dummy");
        return Collections.singletonList(user);
    }

    @Override
    public List<User> findByEnabled(Pageable page, Boolean enabled) {
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        User user = new User();
        user.setEnabled(enabled);
        Example<User> queryExample = Example.of(user, matcher);
        return userRepository.findAll(queryExample, page).getContent();
    }

    @Override
    public List<User> findByEnabledIsTrue() {
        User user = new User();
        user.setUserName("dummy");
        return Collections.singletonList(user);
    }

    @Override
    public boolean save(User user) {
        userRepository.save(user);
        return true;
    }
}