package com.luixtech.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.rpc.webcenter.domain.User;
import com.luixtech.rpc.webcenter.exception.UserDisabledException;
import com.luixtech.rpc.webcenter.exception.UserNotActivatedException;
import com.luixtech.rpc.webcenter.repository.UserAuthorityRepository;
import com.luixtech.rpc.webcenter.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Authenticate a user from the database.
 */
@Service
@Slf4j
public class SpringSecurityUserDetailsServiceImpl implements org.springframework.security.core.userdetails.UserDetailsService {

    private final UserService             userService;
    private final UserAuthorityRepository userAuthorityRepository;

    // Use @Lazy to fix dependencies problems
    public SpringSecurityUserDetailsServiceImpl(@Lazy UserService userService,
                                                UserAuthorityRepository userAuthorityRepository) {
        this.userService = userService;
        this.userAuthorityRepository = userAuthorityRepository;
    }

    @Override
    // @Transactional
    public UserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);
        User userFromDatabase = userService.findOneByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User " + login + " was not found in the database"));

        if (!userFromDatabase.getActivated()) {
            throw new UserNotActivatedException("User " + login + " was not activated");
        }

        if (!Boolean.TRUE.equals(userFromDatabase.getEnabled())) {
            throw new UserDisabledException("User " + login + " was disabled");
        }

        List<GrantedAuthority> grantedAuthorities = userAuthorityRepository.findByUserId(userFromDatabase.getId())
                .stream().map(userAuthority -> new SimpleGrantedAuthority(userAuthority.getAuthorityName()))
                .collect(Collectors.toList());
        return new org.springframework.security.core.userdetails.User(userFromDatabase.getUserName(),
                userFromDatabase.getPasswordHash(), grantedAuthorities);
    }
}
