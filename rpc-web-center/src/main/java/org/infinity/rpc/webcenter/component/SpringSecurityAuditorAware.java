package org.infinity.rpc.webcenter.component;

import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.utils.SecurityUtils;
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
        String userName = SecurityUtils.getCurrentUserName();
        return Optional.of(userName != null ? userName : Authority.SYSTEM_ACCOUNT);
    }
}
