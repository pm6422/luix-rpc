package com.luixtech.rpc.webcenter.controller;

import com.luixtech.framework.component.HttpHeaderCreator;
import com.luixtech.rpc.webcenter.config.ApplicationProperties;
import com.luixtech.rpc.webcenter.domain.Authority;
import com.luixtech.rpc.webcenter.domain.User;
import com.luixtech.rpc.webcenter.domain.UserAuthority;
import com.luixtech.rpc.webcenter.domain.UserProfilePhoto;
import com.luixtech.rpc.webcenter.dto.ManagedUserDTO;
import com.luixtech.rpc.webcenter.dto.UsernameAndPasswordDTO;
import com.luixtech.rpc.webcenter.event.LogoutEvent;
import com.luixtech.rpc.webcenter.exception.NoAuthorityException;
import com.luixtech.rpc.webcenter.repository.UserAuthorityRepository;
import com.luixtech.rpc.webcenter.repository.UserProfilePhotoRepository;
import com.luixtech.rpc.webcenter.service.MailService;
import com.luixtech.rpc.webcenter.service.UserService;
import com.luixtech.rpc.webcenter.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.luixtech.framework.config.api.SpringDocConfiguration.AUTH;
import static com.luixtech.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;
import static com.luixtech.rpc.webcenter.utils.NetworkUtils.getRequestUrl;

/**
 * REST controller for managing users.
 */
@RestController
@SecurityRequirement(name = AUTH)
@AllArgsConstructor
@Slf4j
public class UserController {

    private final ApplicationProperties      applicationProperties;
    private final UserProfilePhotoRepository userProfilePhotoRepository;
    private final UserAuthorityRepository    userAuthorityRepository;
    private final UserService                userService;
    private final MailService                mailService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HttpHeaderCreator         httpHeaderCreator;

    @Operation(summary = "create new user and send activation email")
    @PostMapping("/api/users")
    @PreAuthorize("hasAuthority(\"" + Authority.ADMIN + "\")")
    public ResponseEntity<Void> create(@Parameter(description = "user", required = true) @Valid @RequestBody User domain,
                                       HttpServletRequest request) {
        log.debug("REST request to create user: {}", domain);
        User newUser = userService.insert(domain, applicationProperties.getAccount().getDefaultPassword());
        mailService.sendCreationEmail(newUser, getRequestUrl(request));
        HttpHeaders headers = httpHeaderCreator.createSuccessHeader("NM2011", applicationProperties.getAccount().getDefaultPassword());
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).build();
    }

    @Operation(summary = "find user list")
    @GetMapping("/api/users")
    @PreAuthorize("hasAuthority(\"" + Authority.ADMIN + "\")")
    public ResponseEntity<List<User>> find(@ParameterObject Pageable pageable,
                                           @Parameter(description = "search criteria") @RequestParam(value = "login", required = false) String login) {
        Page<User> users = userService.findByLogin(pageable, login);
        return ResponseEntity.ok().headers(generatePageHeaders(users)).body(users.getContent());
    }

    @Operation(summary = "find user by name")
    @GetMapping("/api/users/{username:[a-zA-Z0-9-]+}")
    @PreAuthorize("hasAuthority(\"" + Authority.ADMIN + "\")")
    public ResponseEntity<ManagedUserDTO> findByName(@Parameter(description = "username", required = true) @PathVariable String username) {
        User domain = userService.findOneByUsername(username);
        List<UserAuthority> userAuthorities = Optional.ofNullable(userAuthorityRepository.findByUserId(domain.getId()))
                .orElseThrow(() -> new NoAuthorityException(username));
        Set<String> authorities = userAuthorities.stream().map(UserAuthority::getAuthorityName).collect(Collectors.toSet());
        return ResponseEntity.ok(new ManagedUserDTO(domain, authorities));
    }

    @Operation(summary = "update user")
    @PutMapping("/api/users")
    @PreAuthorize("hasAuthority(\"" + Authority.ADMIN + "\")")
    public ResponseEntity<Void> update(@Parameter(description = "new user", required = true) @Valid @RequestBody User domain) {
        log.debug("REST request to update user: {}", domain);
        userService.update(domain);
        if (domain.getUsername().equals(SecurityUtils.getCurrentUsername())) {
            // Logout if current user were changed
            applicationEventPublisher.publishEvent(new LogoutEvent(this));
        }
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getUsername())).build();
    }

    @Operation(summary = "delete user by name", description = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/users/{username:[a-zA-Z0-9-]+}")
    @PreAuthorize("hasAuthority(\"" + Authority.ADMIN + "\")")
    public ResponseEntity<Void> delete(@Parameter(description = "username", required = true) @PathVariable String username) {
        log.debug("REST request to delete user: {}", username);
        userService.deleteByUsername(username);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", username)).build();
    }

    @Operation(summary = "reset password by user name")
    @PutMapping("/api/users/{username:[a-zA-Z0-9-]+}")
    @PreAuthorize("hasAuthority(\"" + Authority.ADMIN + "\")")
    public ResponseEntity<Void> resetPassword(@Parameter(description = "user name", required = true) @PathVariable String username) {
        log.debug("REST reset the password of user: {}", username);
        UsernameAndPasswordDTO dto = UsernameAndPasswordDTO.builder()
                .username(username)
                .newPassword(applicationProperties.getAccount().getDefaultPassword()).build();
        userService.changePassword(dto);
        HttpHeaders headers = httpHeaderCreator.createSuccessHeader("NM2012", applicationProperties.getAccount().getDefaultPassword());
        return ResponseEntity.ok().headers(headers).build();
    }

    public static final String GET_PROFILE_PHOTO_URL = "/api/users/profile-photo/";

    @Operation(summary = "get user profile picture")
    @GetMapping(GET_PROFILE_PHOTO_URL + "{username:[a-zA-Z0-9-]+}")
    public ResponseEntity<byte[]> getProfilePhoto(@Parameter(description = "user name", required = true) @PathVariable String username) {
        User user = userService.findOneByUsername(username);
        Optional<UserProfilePhoto> userProfilePhoto = userProfilePhotoRepository.findByUserId(user.getId());
        return userProfilePhoto.map(photo -> ResponseEntity.ok(photo.getProfilePhoto().getData())).orElse(null);
    }
}
