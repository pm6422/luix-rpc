package org.infinity.luix.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.server.annotation.RpcProvider;
import org.infinity.luix.democommon.domain.User;
import org.infinity.luix.democommon.service.UserService;
import org.infinity.luix.demoserver.repository.UserRepository;
import org.springframework.util.Assert;

import javax.annotation.Resource;
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
}