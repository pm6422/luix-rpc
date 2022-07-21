package com.luixtech.rpc.webcenter.config;

import com.luixtech.rpc.webcenter.security.jwt.JwtFilterConfigurer;
import com.luixtech.rpc.webcenter.security.jwt.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.CorsFilter;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

import javax.annotation.Resource;

import static com.luixtech.rpc.webcenter.domain.Authority.ADMIN;
import static com.luixtech.rpc.webcenter.domain.Authority.DEVELOPER;

/**
 * If any class extends WebSecurityConfigurerAdapter, the auto-configuration of spring security will don't work.
 * <p>
 * Refer to https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-Security-2.0
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Import(SecurityProblemSupport.class)
public class SecurityConfiguration {
    @Resource
    private ApplicationProperties  applicationProperties;
    @Resource
    private JwtTokenProvider       jwtTokenProvider;
    @Resource
    private CorsFilter             corsFilter;
    @Resource
    private SecurityProblemSupport problemSupport;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web ->
                web
                        .ignoring()
                        .antMatchers(HttpMethod.OPTIONS, "/**")
                        .antMatchers("/app/**/*.{js,html}")
                        .antMatchers("/content/**")
                        .antMatchers("/favicon.png") // Note: it will cause authorization failure if loss this statement.
                        .antMatchers("/swagger-ui/index.html");
    }

    /**
     * Note: csrf is activated by default when using WebSecurityConfigurerAdapter.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf()
                .disable()
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint(problemSupport)
                .accessDeniedHandler(problemSupport)
                .and()
                .headers()
                .contentSecurityPolicy(applicationProperties.getSecurity().getContentSecurityPolicy())
                .and()
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
                .permissionsPolicy().policy("camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()")
                .and()
                .frameOptions()
                .deny()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/open-api/**").permitAll()
                .antMatchers("/api/**").authenticated()
                .antMatchers("/api/http-sessions/**").hasAuthority(DEVELOPER)
                .antMatchers("/api/user-audit-events/**").hasAuthority(DEVELOPER)
                .antMatchers("/websocket/**").authenticated()
                .antMatchers("/management/health").permitAll()
                .antMatchers("/management/health/**").permitAll()
                .antMatchers("/management/info").permitAll()
                .antMatchers("/management/prometheus").permitAll()
                .antMatchers("/management/**").hasAuthority(ADMIN)
                .and()
                .httpBasic()
                .and()
                .apply(securityConfigurerAdapter());
        return http.build();
    }

    private JwtFilterConfigurer securityConfigurerAdapter() {
        return new JwtFilterConfigurer(jwtTokenProvider);
    }
}
