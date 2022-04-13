package com.luixtech.rpc.webcenter.utils;

import com.luixtech.rpc.webcenter.domain.Authority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

/**
 * Utility class for Spring Security.
 */
public abstract class SecurityUtils {

    /**
     * Get the name of the current user.
     */
    public static String getCurrentUserName() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        String userName = null;
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
                userName = springSecurityUser.getUsername();
            } else if (authentication.getPrincipal() instanceof String) {
                userName = (String) authentication.getPrincipal();
            }
        }
        return userName;
    }

    /**
     * Check if a user is authenticated.
     *
     * @return {@code true} if it was authenticated and {@code false} otherwise
     */
    public static boolean isAuthenticated() {
        Collection<? extends GrantedAuthority> authorities = SecurityUtils.getCurrentUserRoles();
        if (!CollectionUtils.isEmpty(authorities)) {
            for (GrantedAuthority authority : authorities) {
                if (authority.getAuthority().equals(Authority.ANONYMOUS)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return the current user roles.
     *
     * @return the current user roles
     */
    public static Collection<? extends GrantedAuthority> getCurrentUserRoles() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    }

    /**
     * If the current user has a specific authority (security role).
     *
     * <p>
     * The name of this method comes from the isUserInRole() method in the Servlet API
     * </p>
     *
     * @param role the authority to check
     * @return {@code true} if it the current user has the authority and {@code false} otherwise
     */
    public static boolean isCurrentUserInRole(String role) {
        Collection<? extends GrantedAuthority> authorities = SecurityUtils.getCurrentUserRoles();
        if (!CollectionUtils.isEmpty(authorities)) {
            return authorities.contains(new SimpleGrantedAuthority(role));
        }
        return false;
    }
}
