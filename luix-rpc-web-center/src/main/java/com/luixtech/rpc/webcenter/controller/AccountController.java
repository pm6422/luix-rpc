package com.luixtech.rpc.webcenter.controller;

import com.luixtech.rpc.webcenter.dto.ProfileScopeUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.luixtech.rpc.webcenter.config.WebServerSecurityConfiguration.REGISTRATION_ID;

/**
 * REST controller for managing the user's account.
 */
@RestController
@Slf4j
@AllArgsConstructor
public class AccountController {
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Operation(summary = "check if the user is authenticated, and return its login")
    @GetMapping("/api/authenticate")
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    @Operation(summary = "get authenticated user")
    @GetMapping("/open-api/accounts/user")
    public ResponseEntity<ProfileScopeUser> getUser(Principal principal) {
        Map<String, Object> attributes;
        if (principal != null) {
            if (principal instanceof OAuth2AuthenticationToken authenticationToken) {
                attributes = authenticationToken.getPrincipal().getAttributes();
                ProfileScopeUser user = new ProfileScopeUser();
                user.setUsername(attributes.get("username").toString());
                user.setEmail(attributes.get("email").toString());
                user.setAuthorities(Set.copyOf((ArrayList) attributes.get("roles")));
                return ResponseEntity.ok(user);
            }
//            else if (principal instanceof JwtAuthenticationToken authenticationToken) {
//                attributes = authenticationToken.getTokenAttributes();
//            }
            else {
                throw new IllegalArgumentException("AuthenticationToken is not OAuth2 or JWT!");
            }
        }
        return null;
    }

    @Operation(summary = "logout")
    @PostMapping("/api/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, @AuthenticationPrincipal(expression = "idToken") OidcIdToken idToken) {
        request.getSession().invalidate();
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID);
        String logoutUrl = UriComponentsBuilder.fromHttpUrl(registration.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint").toString())
                .queryParam("id_token_hint", idToken.getTokenValue())
                .queryParam("client_id", registration.getClientId())
                .queryParam("post_logout_redirect_uri", request.getHeader(HttpHeaders.ORIGIN))
                .build()
                .toUriString();
        return ResponseEntity.ok().body(Map.of("logoutUrl", logoutUrl));
    }
}
