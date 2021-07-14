package org.infinity.rpc.democommon.service;

import org.infinity.rpc.democommon.domain.User;

import java.util.Optional;

public interface UserService {

    User findOneByLogin(String login);
}