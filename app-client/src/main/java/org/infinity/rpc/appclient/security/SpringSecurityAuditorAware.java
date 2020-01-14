package org.infinity.rpc.appclient.security;

import org.infinity.rpc.appclient.domain.Authority;
import org.infinity.rpc.appclient.utils.SecurityUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementation of AuditorAware based on Spring Security.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        String userName = SecurityUtils.getCurrentUserName();
        return Optional.of(userName != null ? userName : Authority.SYSTEM_ACCOUNT);
    }
}
