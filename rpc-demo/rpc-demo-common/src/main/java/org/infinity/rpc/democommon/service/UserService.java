package org.infinity.rpc.democommon.service;

import org.infinity.rpc.democommon.domain.User;

import java.util.Optional;

public interface UserService {

    Optional<User> findOneByLogin(String login);
}