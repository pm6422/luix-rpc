package com.luixtech.rpc.webcenter.security.jwt;

import com.luixtech.rpc.webcenter.config.ApplicationProperties;
import com.luixtech.rpc.webcenter.service.SecurityErrorMeterService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.security.Key;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
public class JwtTokenProvider {
    private static final String                    AUTHORITIES_KEY = "auth";
    private final        Key                       key;
    private final        JwtParser                 jwtParser;
    private final        long                      tokenValidityInMillis;
    private final        long                      tokenValidityInMillisForRememberMe;
    private final        SecurityErrorMeterService securityErrorMeterService;

    public JwtTokenProvider(ApplicationProperties applicationProperties, SecurityErrorMeterService securityErrorMeterService) {
        byte[] keyBytes = Decoders.BASE64.decode(applicationProperties.getSecurity().getAuthentication().getJwt().getBase64Secret());
        key = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
        this.tokenValidityInMillis = 1000 * applicationProperties.getSecurity().getAuthentication().getJwt()
                .getTokenValidityInSeconds();
        this.tokenValidityInMillisForRememberMe = 1000 * applicationProperties.getSecurity().getAuthentication().getJwt()
                .getTokenValidityInSecondsForRememberMe();
        this.securityErrorMeterService = securityErrorMeterService;
    }

    public String createToken(Authentication user, boolean rememberMe) {
        String authorities = user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        Instant validity = rememberMe ?
                Instant.now().plusMillis(tokenValidityInMillisForRememberMe) :
                Instant.now().plusMillis(tokenValidityInMillis);
        return Jwts
                .builder()
                .setSubject(user.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(Date.from(validity))
                .compact();
    }

    public Authentication extractAuthentication(String jwtToken) {
        Claims claims = jwtParser.parseClaimsJws(jwtToken).getBody();
        Collection<? extends GrantedAuthority> authorities = Arrays
                .stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                .filter(auth -> StringUtils.isNotEmpty(auth.trim()))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        User user = new User(claims.getSubject(), StringUtils.EMPTY, authorities);
        return new UsernamePasswordAuthenticationToken(user, jwtToken, authorities);
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
