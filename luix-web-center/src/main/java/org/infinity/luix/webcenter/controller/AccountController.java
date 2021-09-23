package org.infinity.luix.webcenter.controller;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.infinity.luix.webcenter.component.HttpHeaderCreator;
import org.infinity.luix.webcenter.config.oauth2.SecurityUser;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.domain.User;
import org.infinity.luix.webcenter.domain.UserAuthority;
import org.infinity.luix.webcenter.domain.UserProfilePhoto;
import org.infinity.luix.webcenter.dto.ManagedUserDTO;
import org.infinity.luix.webcenter.dto.ResetKeyAndPasswordDTO;
import org.infinity.luix.webcenter.dto.UserNameAndPasswordDTO;
import org.infinity.luix.webcenter.event.LogoutEvent;
import org.infinity.luix.webcenter.exception.NoAuthorityException;
import org.infinity.luix.webcenter.exception.DataNotFoundException;
import org.infinity.luix.webcenter.repository.UserAuthorityRepository;
import org.infinity.luix.webcenter.repository.UserProfilePhotoRepository;
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
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
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
    private static final FastDateFormat             DATETIME_FORMAT = FastDateFormat.getInstance("yyyyMMdd-HHmmss");
    @Resource
    private              UserService                userService;
    @Resource
    private              UserAuthorityRepository    userAuthorityRepository;
    @Resource
    private              UserProfilePhotoRepository userProfilePhotoRepository;
    @Resource
    private              UserProfilePhotoService    userProfilePhotoService;
    @Resource
    private              AuthorityService           authorityService;
    @Resource
    private              MailService                mailService;
    @Resource
    private              TokenStore                 tokenStore;
    @Resource
    private              ApplicationEventPublisher  applicationEventPublisher;
    @Resource
    private              HttpHeaderCreator          httpHeaderCreator;

    @ApiOperation(value = "retrieve access token", notes = "successful login returns the current access token")
    @GetMapping("/api/accounts/access-token")
    public ResponseEntity<String> getAccessToken(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        if (StringUtils.isEmpty(token) || !token.toLowerCase().startsWith(OAuth2AccessToken.BEARER_TYPE.toLowerCase())) {
            return ResponseEntity.ok(StringUtils.EMPTY);
        }
        return ResponseEntity.ok(StringUtils.substringAfter(token.toLowerCase(), OAuth2AccessToken.BEARER_TYPE.toLowerCase()).trim());
    }

    @ApiOperation(value = "retrieve the currently logged in user name", notes = "successful login returns the current user name")
    @GetMapping("/api/accounts/authenticate")
    public ResponseEntity<String> isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return ResponseEntity.ok(request.getRemoteUser());
    }

    /**
     * Used for SSO client calls, theoretically it will not return null,
     * because an error will occur if you are not logged in,
     * and the current user will be returned to the current user successfully.
     *
     * @param user user
     * @return principal
     */
    @ApiOperation(value = "retrieve the currently logged in user name")
    @GetMapping("/api/accounts/principal")
    public ResponseEntity<Principal> getPrincipal(Principal user) {
        log.debug("REST request to get current user if the user is authenticated");
        return ResponseEntity.ok(user);
    }

    @ApiOperation("retrieve current user")
    @GetMapping("/api/accounts/user")
    @Secured({Authority.USER})
    public ResponseEntity<User> getCurrentUser() {
        User user = userService.findOneByUserName(SecurityUtils.getCurrentUserName());
        List<UserAuthority> userAuthorities = Optional.ofNullable(userAuthorityRepository.findByUserId(user.getId()))
                .orElseThrow(() -> new NoAuthorityException(SecurityUtils.getCurrentUserName()));
        Set<String> authorities = userAuthorities.stream().map(UserAuthority::getAuthorityName).collect(Collectors.toSet());
        user.setAuthorities(authorities);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Signed-In", "true");
        return ResponseEntity.ok().headers(headers).body(user);
    }

    @ApiOperation("retrieve the bound user based on the access token")
    @GetMapping("/open-api/accounts/user")
    public ResponseEntity<Object> getTokenUser(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        if (token != null && token.toLowerCase().startsWith(OAuth2AccessToken.BEARER_TYPE.toLowerCase())) {
            OAuth2Authentication oAuth2Authentication = tokenStore.readAuthentication(StringUtils
                    .substringAfter(token.toLowerCase(), OAuth2AccessToken.BEARER_TYPE.toLowerCase()).trim());
            if (oAuth2Authentication != null) {
                User user = userService.findOneByUserName(oAuth2Authentication.getUserAuthentication().getName());
                Set<String> authorities = oAuth2Authentication.getUserAuthentication().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
                if (user != null) {
                    user.setAuthorities(authorities);
                    return ResponseEntity.ok(user);
                }
            }
        }
        // UserInfoTokenServices.loadAuthentication里会判断是否返回结果里包含error字段值，如果返回null会有空指针异常
        // 这个也许是客户端的一个BUG，升级后观察是否已经修复
        return ResponseEntity.ok(ImmutableMap.of("error", true));
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
    @Secured({Authority.USER})
    public ResponseEntity<List<String>> getAuthorityNames(
            @ApiParam(allowableValues = "false,true,null") @RequestParam(value = "enabled", required = false) Boolean enabled) {
        List<String> authorities = authorityService.find(enabled).stream().map(Authority::getName).collect(Collectors.toList());
        return ResponseEntity.ok(authorities);
    }

    @ApiOperation("update current user")
    @PutMapping("/api/accounts/user")
    @Secured({Authority.USER})
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
    @Secured({Authority.USER})
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
    @Secured({Authority.USER})
    public void uploadProfilePhoto(@ApiParam(value = "file Description", required = true) @RequestPart String description,
                                   @ApiParam(value = "user profile picture", required = true) @RequestPart MultipartFile file) throws IOException {
        log.debug("Upload profile with file name {} and description {}", file.getOriginalFilename(), description);
        User user = userService.findOneByUserName(SecurityUtils.getCurrentUserName());
        userProfilePhotoService.save(user, file.getBytes());
    }

    @ApiOperation("download user profile picture")
    @GetMapping("/api/accounts/profile-photo/download")
    @Secured({Authority.USER})
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
    @Secured({Authority.USER})
    public ModelAndView getProfilePhoto() {
        // @RestController下使用return forwardUrl不好使
        String forwardUrl = "forward:".concat(UserController.GET_PROFILE_PHOTO_URL).concat(SecurityUtils.getCurrentUserName());
        log.info(forwardUrl);
        return new ModelAndView(forwardUrl);
    }
}
