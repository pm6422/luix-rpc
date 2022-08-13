package com.luixtech.rpc.webcenter.security;

import com.luixtech.rpc.webcenter.domain.Authority;
import com.luixtech.rpc.webcenter.utils.SecurityUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Implementation of AuditorAware based on Spring Security.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    @Nonnull
    public Optional<String> getCurrentAuditor() {
        String username = SecurityUtils.getCurrentUsername();
        return Optional.of(username != null ? username : Authority.SYSTEM_ACCOUNT);
    }
}
