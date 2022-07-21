package com.luixtech.rpc.webcenter.security.jwt;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
@AllArgsConstructor
public class JwtFilter extends GenericFilterBean {

    public static final String           AUTHORIZATION_HEADER = "Authorization";
    public static final String           AUTHORIZATION_TOKEN  = "access_token";
    private final       JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String jwtToken = resolveToken(httpServletRequest);
        if (StringUtils.isNotEmpty(jwtToken) && jwtTokenProvider.validateToken(jwtToken)) {
            // Set authentication to security context after validating successfully
            Authentication authentication = jwtTokenProvider.extractAuthentication(jwtToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.isNotEmpty(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return StringUtils.trimToNull(request.getParameter(AUTHORIZATION_TOKEN));
    }
}
