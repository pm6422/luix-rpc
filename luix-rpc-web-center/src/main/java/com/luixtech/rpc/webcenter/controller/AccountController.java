package com.luixtech.rpc.webcenter.controller;

import com.luixtech.rpc.webcenter.component.HttpHeaderCreator;
import com.luixtech.rpc.webcenter.domain.Authority;
import com.luixtech.rpc.webcenter.domain.User;
import com.luixtech.rpc.webcenter.domain.UserAuthority;
import com.luixtech.rpc.webcenter.domain.UserProfilePhoto;
import com.luixtech.rpc.webcenter.dto.LoginDTO;
import com.luixtech.rpc.webcenter.dto.ManagedUserDTO;
import com.luixtech.rpc.webcenter.dto.ResetKeyAndPasswordDTO;
import com.luixtech.rpc.webcenter.dto.UsernameAndPasswordDTO;
import com.luixtech.rpc.webcenter.event.LogoutEvent;
import com.luixtech.rpc.webcenter.exception.DataNotFoundException;
import com.luixtech.rpc.webcenter.repository.UserAuthorityRepository;
import com.luixtech.rpc.webcenter.repository.UserProfilePhotoRepository;
import com.luixtech.rpc.webcenter.security.jwt.JwtFilter;
import com.luixtech.rpc.webcenter.security.jwt.JwtTokenProvider;
import com.luixtech.rpc.webcenter.service.AuthorityService;
import com.luixtech.rpc.webcenter.service.MailService;
import com.luixtech.rpc.webcenter.service.UserProfilePhotoService;
import com.luixtech.rpc.webcenter.service.UserService;
import com.luixtech.rpc.webcenter.utils.RandomUtils;
import com.luixtech.rpc.webcenter.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.luixtech.rpc.webcenter.config.api.SpringDocConfiguration.AUTH;
import static com.luixtech.rpc.webcenter.utils.NetworkUtils.getRequestUrl;

/**
 * REST controller for managing the user's account.
 */
@RestController
@SecurityRequirement(name = AUTH)
@Slf4j
public class AccountController {
    private static final FastDateFormat               DATETIME_FORMAT = FastDateFormat.getInstance("yyyyMMdd-HHmmss");
    private static final String                       ANONYMOUS_USER  = "anonymousUser";
    @Resource
    private              UserService                  userService;
    @Resource
    private              UserAuthorityRepository      userAuthorityRepository;
    @Resource
    private              UserProfilePhotoRepository   userProfilePhotoRepository;
    @Resource
    private              UserProfilePhotoService      userProfilePhotoService;
    @Resource
    private              AuthorityService             authorityService;
    @Resource
    private              MailService                  mailService;
    @Resource
    private              ApplicationEventPublisher    applicationEventPublisher;
    @Resource
    private              HttpHeaderCreator            httpHeaderCreator;
    @Resource
    private              JwtTokenProvider             jwtTokenProvider;
    @Resource
    private              AuthenticationManagerBuilder authenticationManagerBuilder;

    @Operation(summary = "authenticate login")
    @PostMapping("/open-api/accounts/authenticate")
    public ResponseEntity<String> authorize(@Valid @RequestBody LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());
        Authentication user = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        // Set authentication to security context after login successfully
        SecurityContextHolder.getContext().setAuthentication(user);
        String jwt = jwtTokenProvider.createToken(user, loginDTO.isRememberMe());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JwtFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        return ResponseEntity.ok().headers(httpHeaders).body(jwt);
    }

    @Operation(summary = "retrieve current user")
    @GetMapping("/open-api/accounts/user")
    public ResponseEntity<User> getCurrentUser() {
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (StringUtils.isEmpty(currentUsername) || ANONYMOUS_USER.equals(currentUsername)) {
            return null;
        }
        User user = userService.findOneByUsername(currentUsername);
        List<UserAuthority> userAuthorities = userAuthorityRepository.findByUserId(user.getId());
        if (CollectionUtils.isNotEmpty(userAuthorities)) {
            Set<String> authorities = userAuthorities.stream().map(UserAuthority::getAuthorityName).collect(Collectors.toSet());
            user.setAuthorities(authorities);
        }
        return ResponseEntity.ok().body(user);
    }

    @Operation(summary = "logout")
    @PostMapping("/api/accounts/logout")
    public void logout() {
        applicationEventPublisher.publishEvent(new LogoutEvent(this));
    }

    @Operation(summary = "register a new user and send an activation email")
    @PostMapping("/open-api/accounts/register")
    public ResponseEntity<Void> registerAccount(
            @Parameter(description = "user", required = true) @Valid @RequestBody ManagedUserDTO dto,
            HttpServletRequest request) {
        log.debug("REST request to register user: {}", dto);
        User newUser = userService.insert(dto.toUser(), dto.getPassword());
        mailService.sendActivationEmail(newUser, getRequestUrl(request));
        HttpHeaders headers = httpHeaderCreator.createSuccessHeader("NM2001");
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).build();
    }

    @Operation(summary = "activate the account according to the activation code")
    @GetMapping("/open-api/accounts/activate/{key:[0-9]+}")
    public void activateAccount(@Parameter(description = "activation code", required = true) @PathVariable String key) {
        userService.activateRegistration(key).orElseThrow(() -> new DataNotFoundException(key));
    }

    @Operation(summary = "retrieve a list of permission values")
    @GetMapping("/api/accounts/authority-names")
    public ResponseEntity<List<String>> getAuthorityNames(
            @Parameter(schema = @Schema(allowableValues = {"false", "true", "null"})) @RequestParam(value = "enabled", required = false) Boolean enabled) {
        List<String> authorities = authorityService.find(enabled).stream().map(Authority::getName).collect(Collectors.toList());
        return ResponseEntity.ok(authorities);
    }

    @Operation(summary = "update current user")
    @PutMapping("/api/accounts/user")
    public ResponseEntity<Void> updateCurrentAccount(@Parameter(description = "new user", required = true) @Valid @RequestBody User domain) {
        // For security reason
        User currentUser = userService.findOneByUsername(SecurityUtils.getCurrentUsername());
        domain.setId(currentUser.getId());
        domain.setUsername(currentUser.getUsername());
        userService.update(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getUsername())).build();
    }

    @Operation(summary = "modify the password of the current user")
    @PutMapping("/api/accounts/password")
    public ResponseEntity<Void> changePassword(@Parameter(description = "new password", required = true) @RequestBody @Valid UsernameAndPasswordDTO dto) {
        // For security reason
        dto.setUsername(SecurityUtils.getCurrentUsername());
        userService.changePassword(dto);
        // Logout asynchronously
        applicationEventPublisher.publishEvent(new LogoutEvent(this));
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", "password")).build();
    }

    @Operation(summary = "send reset password email")
    @PostMapping("/open-api/accounts/reset-password/init")
    public ResponseEntity<Void> requestPasswordReset(@Parameter(description = "email", required = true) @RequestBody String email,
                                                     HttpServletRequest request) {
        User user = userService.requestPasswordReset(email, RandomUtils.generateResetKey());
        mailService.sendPasswordResetMail(user, getRequestUrl(request));
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("NM2002")).build();
    }

    @Operation(summary = "reset password")
    @PostMapping("/open-api/accounts/reset-password/finish")
    public ResponseEntity<Void> finishPasswordReset(@Parameter(description = "reset code and new password", required = true) @Valid @RequestBody ResetKeyAndPasswordDTO dto) {
        userService.completePasswordReset(dto.getNewPassword(), dto.getKey());
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("NM2003")).build();
    }

    @Operation(summary = "upload current user profile picture")
    @PostMapping("/api/accounts/profile-photo/upload")
    public void uploadProfilePhoto(@Parameter(description = "file Description", required = true) @RequestPart String description,
                                   @Parameter(description = "user profile picture", required = true) @RequestPart MultipartFile file) throws IOException {
        log.debug("Upload profile with file name {} and description {}", file.getOriginalFilename(), description);
        User user = userService.findOneByUsername(SecurityUtils.getCurrentUsername());
        userProfilePhotoService.save(user, file.getBytes());
    }

    @Operation(summary = "download user profile picture")
    @GetMapping("/api/accounts/profile-photo/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadProfilePhoto() {
        String currentUserName = SecurityUtils.getCurrentUsername();
        User user = userService.findOneByUsername(currentUserName);
        Optional<UserProfilePhoto> existingPhoto = userProfilePhotoRepository.findByUserId(user.getId());
        if (!existingPhoto.isPresent()) {
            return ResponseEntity.ok().body(null);
        }
        ByteArrayResource resource = new ByteArrayResource(existingPhoto.get().getProfilePhoto().getData());
        String fileName = "profile-" + DATETIME_FORMAT.format(new Date()) + ".jpg";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(existingPhoto.get().getProfilePhoto().getData().length)
                .body(resource);

//        String path = System.getProperty("user.home") + File.separator + "fileName.txt";
//        File outFile = ResourceUtils.getFile(path);
//        FileUtils.writeLines(outFile, strList);
    }

    @Operation(summary = "retrieve the current user avatar")
    @GetMapping("/api/accounts/profile-photo")
    public ModelAndView getProfilePhoto() {
        // @RestController下使用return forwardUrl不好使
        String forwardUrl = "forward:".concat(UserController.GET_PROFILE_PHOTO_URL).concat(SecurityUtils.getCurrentUsername());
        log.info(forwardUrl);
        return new ModelAndView(forwardUrl);
    }
}
