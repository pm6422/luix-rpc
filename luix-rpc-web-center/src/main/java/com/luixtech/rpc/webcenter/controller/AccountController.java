package com.luixtech.rpc.webcenter.controller;

import com.luixtech.rpc.webcenter.config.ApplicationProperties;
import com.luixtech.rpc.webcenter.dto.ProfileScopeUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

/**
 * REST controller for managing the user's account.
 */
@RestController
@Slf4j
public class AccountController {
    private final WebClient             webClient;
    private final ApplicationProperties applicationProperties;
    private final ClientRegistration    registration;

    public AccountController(WebClient webClient, ApplicationProperties applicationProperties,
                             ClientRegistrationRepository registrations) {
        this.webClient = webClient;
        this.applicationProperties = applicationProperties;
        this.registration = registrations.findByRegistrationId("messaging-client-oidc");
    }

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

    @Operation(summary = "logout")
    @PostMapping("/api/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, @AuthenticationPrincipal(expression = "idToken") OidcIdToken idToken) {
        StringBuilder logoutUrl = new StringBuilder();
        logoutUrl.append(this.registration.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint").toString());
        logoutUrl.append("?id_token_hint=").append(idToken.getTokenValue());
        logoutUrl.append("&post_logout_redirect_uri=").append(request.getHeader(HttpHeaders.ORIGIN));
        request.getSession().invalidate();
        return ResponseEntity.ok().body(Map.of("logoutUrl", logoutUrl.toString()));
    }
}
