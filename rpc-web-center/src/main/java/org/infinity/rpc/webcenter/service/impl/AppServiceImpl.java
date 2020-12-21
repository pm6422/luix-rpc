package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.webcenter.domain.App;
import org.infinity.rpc.webcenter.domain.AppAuthority;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.AppAuthorityRepository;
import org.infinity.rpc.webcenter.repository.AppRepository;
import org.infinity.rpc.webcenter.service.AppService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AppServiceImpl implements AppService {

    private final AppRepository appRepository;

    private final AppAuthorityRepository appAuthorityRepository;

    public AppServiceImpl(AppRepository appRepository, AppAuthorityRepository appAuthorityRepository) {
        this.appRepository = appRepository;
        this.appAuthorityRepository = appAuthorityRepository;
    }

    @Override
    public App insert(App domain) {
        appRepository.save(domain);
        domain.getAuthorities().forEach(authorityName -> appAuthorityRepository.insert(new AppAuthority(domain.getName(), authorityName)));
        log.debug("Created Information for app: {}", domain);
        return domain;
    }

    @Override
    public void update(App domain) {
        appRepository.findById(domain.getName()).map(app -> {
            app.setEnabled(domain.getEnabled());
            appRepository.save(app);
            log.debug("Updated app: {}", app);

            if (CollectionUtils.isNotEmpty(domain.getAuthorities())) {
                appAuthorityRepository.deleteByAppName(domain.getName());
                domain.getAuthorities().forEach(authorityName -> appAuthorityRepository.insert(new AppAuthority(domain.getName(), authorityName)));
                log.debug("Updated user authorities");
            } else {
                appAuthorityRepository.deleteByAppName(app.getName());
            }
            return app;
        }).orElseThrow(() -> new NoDataFoundException(domain.getName()));
    }
}