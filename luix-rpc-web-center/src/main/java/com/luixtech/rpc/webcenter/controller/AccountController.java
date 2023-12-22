package com.luixtech.rpc.webcenter.controller;

import com.luixtech.rpc.webcenter.config.ApplicationProperties;
import com.luixtech.rpc.webcenter.dto.ProfileScopeUser;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

/**
 * REST controller for managing the user's account.
 */
@RestController
@AllArgsConstructor
@Slf4j
public class AccountController {
    private final WebClient             webClient;
    private final ApplicationProperties applicationProperties;

    @Operation(summary = "get authenticated user")
    @GetMapping("/open-api/accounts/user")
    public ResponseEntity<ProfileScopeUser> getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            ProfileScopeUser user = this.webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(applicationProperties.getUrl().getAuthServerUserUrl())
                            .queryParam("username", oidcUser.getName())
                            .build())
                    .attributes(clientRegistrationId("messaging-client-oidc"))
                    .retrieve()
                    .bodyToMono(ProfileScopeUser.class)
                    .block();
            return ResponseEntity.ok(user);
        }
        return null;
    }
}
