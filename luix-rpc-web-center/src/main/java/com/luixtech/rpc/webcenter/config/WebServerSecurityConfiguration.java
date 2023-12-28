package com.luixtech.rpc.webcenter.config;

import com.luixtech.rpc.webcenter.config.security.CsrfRequestMatcher;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
@AllArgsConstructor
public class WebServerSecurityConfiguration {
    public static final String                       REGISTRATION_ID = "luix-passport-client-oidc";
    private final       ClientRegistrationRepository clientRegistrationRepository;

    // @formatter:off
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize ->
				authorize
					.requestMatchers("/management/health/**").permitAll()
					.anyRequest().authenticated()
			)
			.csrf(csrf-> csrf
				// Ignore matching requests
				.ignoringRequestMatchers("/api/logout")
				// Solve post/delete forbidden issue for request from swagger
				.requireCsrfProtectionMatcher(new CsrfRequestMatcher()))
			// Solved swagger denied issue embedded in i-frame
		    .headers(headers-> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
			// Default OAuth 2.0 Login Page should match the format /oauth2/authorization/{registrationId}
			// See {@link org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI}
			.oauth2Login(oauth2Login -> oauth2Login.loginPage("/oauth2/authorization/" + REGISTRATION_ID))
			.oauth2Client(withDefaults());
//			.logout(logout -> logout.logoutSuccessHandler(new CustomLogoutSuccessHandler(clientRegistrationRepository)));
		return http.build();
	}
	// @formatter:on
}
