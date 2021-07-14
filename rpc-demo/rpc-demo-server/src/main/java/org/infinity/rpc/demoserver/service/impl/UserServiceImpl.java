package org.infinity.rpc.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.server.annotation.RpcProvider;
import org.infinity.rpc.democommon.domain.User;
import org.infinity.rpc.democommon.service.UserService;
import org.infinity.rpc.demoserver.repository.UserRepository;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Locale;
import java.util.Optional;

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