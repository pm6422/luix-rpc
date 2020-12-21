package org.infinity.rpc.appserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.domain.App;
import org.infinity.app.common.service.AppService;
import org.infinity.rpc.appserver.exception.NoDataFoundException;
import org.infinity.rpc.appserver.repository.AppRepository;
import org.infinity.rpc.core.server.annotation.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Provider(maxRetries = 1)
@Slf4j
public class AppServiceImpl implements AppService {

    private final AppRepository appRepository;

    public AppServiceImpl(AppRepository appRepository) {
        this.appRepository = appRepository;
    }

    @Override
    public Page<App> findAll(Pageable pageable) {
        return appRepository.findAll(pageable);
    }

    @Override
    public Optional<App> findById(String id) {
        return appRepository.findById(id);
    }

    @Override
    public App insert(App domain) {
        appRepository.save(domain);
        log.debug("Created Information for app: {}", domain);
        return domain;
    }

    @Override
    public void update(App domain) {
        appRepository.findById(domain.getName()).map(app -> {
            app.setEnabled(domain.getEnabled());
            appRepository.save(app);
            log.debug("Updated app: {}", app);
            return app;
        }).orElseThrow(() -> new NoDataFoundException(domain.getName()));
    }

    @Override
    public void deleteById(String id) {
        appRepository.deleteById(id);
    }
}