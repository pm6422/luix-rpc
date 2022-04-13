package com.luixtech.luixrpc.webcenter.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.luixrpc.webcenter.config.ApplicationProperties;
import com.luixtech.luixrpc.webcenter.service.SecurityErrorMeterService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TokenProvider {
    private static final String                    AUTHORITIES_KEY = "auth";
    private final        Key                       key;
    private final        JwtParser                 jwtParser;
    private final        long                      tokenValidityInMilliseconds;
    private final        long                      tokenValidityInMillisecondsForRememberMe;
    private final        SecurityErrorMeterService securityErrorMeterService;

    public TokenProvider(ApplicationProperties applicationProperties, SecurityErrorMeterService securityErrorMeterService) {
        byte[] keyBytes = Decoders.BASE64.decode(applicationProperties.getSecurity().getAuthentication().getJwt().getBase64Secret());
        key = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
        this.tokenValidityInMilliseconds = 1000 * applicationProperties.getSecurity().getAuthentication().getJwt()
                .getTokenValidityInSeconds();
        this.tokenValidityInMillisecondsForRememberMe =
                1000 * applicationProperties.getSecurity().getAuthentication().getJwt()
                        .getTokenValidityInSecondsForRememberMe();
        this.securityErrorMeterService = securityErrorMeterService;
    }

    public String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity;
        if (rememberMe) {
            validity = new Date(now + this.tokenValidityInMillisecondsForRememberMe);
        } else {
            validity = new Date(now + this.tokenValidityInMilliseconds);
        }

        return Jwts
                .builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        Collection<? extends GrantedAuthority> authorities = Arrays
                .stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                .filter(auth -> StringUtils.isNotEmpty(auth.trim()))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        User principal = new User(claims.getSubject(), StringUtils.EMPTY, authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean validateToken(String authToken) {
        try {
            jwtParser.parseClaimsJws(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            this.securityErrorMeterService.trackTokenExpired();
            log.warn("Invalid JWT token.", e);
        } catch (UnsupportedJwtException e) {
            this.securityErrorMeterService.trackTokenUnsupported();
            log.warn("Invalid JWT token.", e);
        } catch (MalformedJwtException e) {
            this.securityErrorMeterService.trackTokenMalformed();
            log.warn("Invalid JWT token.", e);
        } catch (SignatureException e) {
            this.securityErrorMeterService.trackTokenInvalidSignature();
            log.warn("Invalid JWT token.", e);
        } catch (IllegalArgumentException e) {
            // TODO: should we let it bubble (no catch), to avoid defensive programming and follow the fail-fast principle?
            log.error("Token validation error {}", e.getMessage());
        }
        return false;
    }
}
