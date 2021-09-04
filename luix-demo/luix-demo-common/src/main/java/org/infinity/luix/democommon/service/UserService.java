package org.infinity.luix.democommon.service;

import org.infinity.luix.democommon.domain.User;

public interface UserService {

    User findOneByLogin(String login);
}