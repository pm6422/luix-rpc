package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.component.HttpHeaderCreator;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.domain.User;
import org.infinity.rpc.webcenter.domain.UserAuthority;
import org.infinity.rpc.webcenter.domain.UserProfilePhoto;
import org.infinity.rpc.webcenter.dto.ManagedUserDTO;
import org.infinity.rpc.webcenter.dto.UserNameAndPasswordDTO;
import org.infinity.rpc.webcenter.event.LogoutEvent;
import org.infinity.rpc.webcenter.exception.NoAuthorityException;
import org.infinity.rpc.webcenter.repository.UserAuthorityRepository;
import org.infinity.rpc.webcenter.repository.UserProfilePhotoRepository;
import org.infinity.rpc.webcenter.service.MailService;
import org.infinity.rpc.webcenter.service.UserService;
import org.infinity.rpc.webcenter.utils.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.infinity.rpc.webcenter.domain.User.DEFAULT_PASSWORD;
import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;
import static org.infinity.rpc.webcenter.utils.NetworkUtils.getRequestUrl;

/**
 * REST controller for managing users.
 */
@RestController
@Slf4j
public class UserController {

    @Resource
    private UserProfilePhotoRepository userProfilePhotoRepository;
    @Resource
    private UserAuthorityRepository    userAuthorityRepository;
    @Resource
    private UserService                userService;
    @Resource
    private MailService                mailService;
    @Resource
    private ApplicationEventPublisher  applicationEventPublisher;
    @Resource
    private HttpHeaderCreator          httpHeaderCreator;

    @ApiOperation(value = "create new user and send activation email")
    @PostMapping("/api/users")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> create(@ApiParam(value = "user", required = true) @Valid @RequestBody User domain,
                                       HttpServletRequest request) {
        log.debug("REST request to create user: {}", domain);
        User newUser = userService.insert(domain, DEFAULT_PASSWORD);
        mailService.sendCreationEmail(newUser, getRequestUrl(request));
        HttpHeaders headers = httpHeaderCreator.createSuccessHeader("NM2011", DEFAULT_PASSWORD);
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).build();
    }

    @ApiOperation("find user list")
    @GetMapping("/api/users")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<User>> find(Pageable pageable,
                                           @ApiParam(value = "search criteria") @RequestParam(value = "login", required = false) String login) {
        Page<User> users = userService.findByLogin(pageable, login);
        return ResponseEntity.ok().headers(generatePageHeaders(users)).body(users.getContent());
    }

    @ApiOperation("find user by name")
    @GetMapping("/api/users/{userName:[a-zA-Z0-9-]+}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<ManagedUserDTO> findByName(@ApiParam(value = "user name", required = true) @PathVariable String userName) {
        User domain = userService.findOneByUserName(userName);
        List<UserAuthority> userAuthorities = Optional.ofNullable(userAuthorityRepository.findByUserId(domain.getId()))
                .orElseThrow(() -> new NoAuthorityException(userName));
        Set<String> authorities = userAuthorities.stream().map(UserAuthority::getAuthorityName).collect(Collectors.toSet());
        return ResponseEntity.ok(new ManagedUserDTO(domain, authorities));
    }

    @ApiOperation("update user")
    @PutMapping("/api/users")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> update(@ApiParam(value = "new user", required = true) @Valid @RequestBody User domain) {
        log.debug("REST request to update user: {}", domain);
        userService.update(domain);
        if (domain.getUserName().equals(SecurityUtils.getCurrentUserName())) {
            // Logout if current user were changed
            applicationEventPublisher.publishEvent(new LogoutEvent(this));
        }
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getUserName())).build();
    }

    @ApiOperation(value = "delete user by name", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/users/{userName:[a-zA-Z0-9-]+}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> delete(@ApiParam(value = "user name", required = true) @PathVariable String userName) {
        log.debug("REST request to delete user: {}", userName);
        userService.deleteByUserName(userName);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", userName)).build();
    }

    @ApiOperation("reset password by user name")
    @PutMapping("/api/users/{userName:[a-zA-Z0-9-]+}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<String> resetPassword(@ApiParam(value = "user name", required = true) @PathVariable String userName) {
        log.debug("REST reset the password of user: {}", userName);
        userService.changePassword(UserNameAndPasswordDTO.builder().userName(userName).newPassword(DEFAULT_PASSWORD).build());
        HttpHeaders headers = httpHeaderCreator.createSuccessHeader("NM2012", DEFAULT_PASSWORD);
        return ResponseEntity.ok().headers(headers).body(DEFAULT_PASSWORD);
    }

    public static final String GET_PROFILE_PHOTO_URL = "/api/user/profile-photo/";

    @ApiOperation("get user profile picture")
    @GetMapping(GET_PROFILE_PHOTO_URL + "{userName:[a-zA-Z0-9-]+}")
    @Secured({Authority.USER})
    public ResponseEntity<byte[]> getProfilePhoto(@ApiParam(value = "user name", required = true) @PathVariable String userName) {
        User user = userService.findOneByUserName(userName);
        Optional<UserProfilePhoto> userProfilePhoto = userProfilePhotoRepository.findByUserId(user.getId());
        return userProfilePhoto.map(photo -> ResponseEntity.ok(photo.getProfilePhoto().getData())).orElse(null);
    }
}
