package com.luixtech.rpc.webcenter.controller;

import com.luixtech.rpc.webcenter.dto.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for managing the user's account.
 */
@RestController
@AllArgsConstructor
@Slf4j
public class AccountController {

    @Operation(summary = "get authenticated user")
    @GetMapping("/open-api/accounts/user")
    public ResponseEntity<AuthUser> getProfilePhoto() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            Set<String> authorities = oidcUser.getAuthorities().stream().map(auth -> auth.getAuthority()).collect(Collectors.toSet());
            return ResponseEntity.ok(new AuthUser(oidcUser.getName(), authorities));
        }
        return null;
    }
}
