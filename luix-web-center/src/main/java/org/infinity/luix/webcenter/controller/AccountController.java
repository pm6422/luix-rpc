package org.infinity.luix.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.infinity.luix.webcenter.component.HttpHeaderCreator;
import org.infinity.luix.webcenter.domain.*;
import org.infinity.luix.webcenter.dto.*;
import org.infinity.luix.webcenter.event.LogoutEvent;
import org.infinity.luix.webcenter.exception.DataNotFoundException;
import org.infinity.luix.webcenter.repository.UserAuthorityRepository;
import org.infinity.luix.webcenter.repository.UserProfilePhotoRepository;
import org.infinity.luix.webcenter.security.jwt.JWTFilter;
import org.infinity.luix.webcenter.security.jwt.TokenProvider;
import org.infinity.luix.webcenter.service.AuthorityService;
import org.infinity.luix.webcenter.service.MailService;
import org.infinity.luix.webcenter.service.UserProfilePhotoService;
import org.infinity.luix.webcenter.service.UserService;
import org.infinity.luix.webcenter.utils.RandomUtils;
import org.infinity.luix.webcenter.utils.SecurityUtils;
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

import static org.infinity.luix.webcenter.utils.NetworkUtils.getRequestUrl;

/**
 * REST controller for managing the user's account.
 */
@RestController
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
    private              TokenProvider                tokenProvider;
    @Resource
    private              AuthenticationManagerBuilder authenticationManagerBuilder;

    @ApiOperation("authenticate login")
    @PostMapping("/open-api/accounts/authenticate")
    public ResponseEntity<String> authorize(@Valid @RequestBody LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(),
                loginDTO.getPassword()
        );

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.createToken(authentication, loginDTO.isRememberMe());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        return new ResponseEntity<>(jwt, httpHeaders, HttpStatus.OK);
    }

    @ApiOperation("logout")
    @PostMapping("/api/accounts/logout")
    public void logout() {
        applicationEventPublisher.publishEvent(new LogoutEvent(this));
    }

    @ApiOperation("retrieve current user")
    @GetMapping("/open-api/accounts/user")
    public ResponseEntity<User> getCurrentUser() {
        String currentUserName = SecurityUtils.getCurrentUserName();
        if (StringUtils.isEmpty(currentUserName) || ANONYMOUS_USER.equals(currentUserName)) {
            return null;
        }
        User user = userService.findOneByUserName(currentUserName);
        List<UserAuthority> userAuthorities = userAuthorityRepository.findByUserId(user.getId());
        if (CollectionUtils.isNotEmpty(userAuthorities)) {
            Set<String> authorities = userAuthorities.stream().map(UserAuthority::getAuthorityName).collect(Collectors.toSet());
            user.setAuthorities(authorities);
        }
        return ResponseEntity.ok().body(user);
    }

    @ApiOperation("register a new user and send an activation email")
    @PostMapping("/open-api/accounts/register")
    public ResponseEntity<Void> registerAccount(
            @ApiParam(value = "user", required = true) @Valid @RequestBody ManagedUserDTO dto,
            HttpServletRequest request) {
        log.debug("REST request to register user: {}", dto);
        User newUser = userService.insert(dto.toUser(), dto.getPassword());
        mailService.sendActivationEmail(newUser, getRequestUrl(request));
        HttpHeaders headers = httpHeaderCreator.createSuccessHeader("NM2001");
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).build();
    }

    @ApiOperation("activate the account according to the activation code")
    @GetMapping("/open-api/accounts/activate/{key:[0-9]+}")
    public void activateAccount(@ApiParam(value = "activation code", required = true) @PathVariable String key) {
        userService.activateRegistration(key).orElseThrow(() -> new DataNotFoundException(key));
    }

    @ApiOperation("retrieve a list of permission values")
    @GetMapping("/api/accounts/authority-names")
    public ResponseEntity<List<String>> getAuthorityNames(
            @ApiParam(allowableValues = "false,true,null") @RequestParam(value = "enabled", required = false) Boolean enabled) {
        List<String> authorities = authorityService.find(enabled).stream().map(Authority::getName).collect(Collectors.toList());
        return ResponseEntity.ok(authorities);
    }

    @ApiOperation("update current user")
    @PutMapping("/api/accounts/user")
    public ResponseEntity<Void> updateCurrentAccount(@ApiParam(value = "new user", required = true) @Valid @RequestBody User domain) {
        // For security reason
        User currentUser = userService.findOneByUserName(SecurityUtils.getCurrentUserName());
        domain.setId(currentUser.getId());
        domain.setUserName(currentUser.getUserName());
        userService.update(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getUserName())).build();
    }

    @ApiOperation("modify the password of the current user")
    @PutMapping("/api/accounts/password")
    public ResponseEntity<Void> changePassword(@ApiParam(value = "new password", required = true) @RequestBody @Valid UserNameAndPasswordDTO dto) {
        // For security reason
        dto.setUserName(SecurityUtils.getCurrentUserName());
        userService.changePassword(dto);
        // Logout asynchronously
        applicationEventPublisher.publishEvent(new LogoutEvent(this));
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", "password")).build();
    }

    @ApiOperation("send reset password email")
    @PostMapping("/open-api/accounts/reset-password/init")
    public ResponseEntity<Void> requestPasswordReset(@ApiParam(value = "email", required = true) @RequestBody String email,
                                                     HttpServletRequest request) {
        User user = userService.requestPasswordReset(email, RandomUtils.generateResetKey());
        mailService.sendPasswordResetMail(user, getRequestUrl(request));
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("NM2002")).build();
    }

    @ApiOperation("reset password")
    @PostMapping("/open-api/accounts/reset-password/finish")
    public ResponseEntity<Void> finishPasswordReset(@ApiParam(value = "reset code and new password", required = true) @Valid @RequestBody ResetKeyAndPasswordDTO dto) {
        userService.completePasswordReset(dto.getNewPassword(), dto.getKey());
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("NM2003")).build();
    }

    @ApiOperation("upload current user profile picture")
    @PostMapping("/api/accounts/profile-photo/upload")
    public void uploadProfilePhoto(@ApiParam(value = "file Description", required = true) @RequestPart String description,
                                   @ApiParam(value = "user profile picture", required = true) @RequestPart MultipartFile file) throws IOException {
        log.debug("Upload profile with file name {} and description {}", file.getOriginalFilename(), description);
        User user = userService.findOneByUserName(SecurityUtils.getCurrentUserName());
        userProfilePhotoService.save(user, file.getBytes());
    }

    @ApiOperation("download user profile picture")
    @GetMapping("/api/accounts/profile-photo/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadProfilePhoto() {
        SecurityUser currentUser = SecurityUtils.getCurrentUser();
        Optional<UserProfilePhoto> existingPhoto = userProfilePhotoRepository.findByUserId(currentUser.getUserId());
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

    @ApiOperation("retrieve the current user avatar")
    @GetMapping("/api/accounts/profile-photo")
    public ModelAndView getProfilePhoto() {
        // @RestController下使用return forwardUrl不好使
        String forwardUrl = "forward:".concat(UserController.GET_PROFILE_PHOTO_URL).concat(SecurityUtils.getCurrentUserName());
        log.info(forwardUrl);
        return new ModelAndView(forwardUrl);
    }
}
