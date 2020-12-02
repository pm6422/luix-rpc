package org.infinity.rpc.appserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.domain.App;
import org.infinity.app.common.service.AppService;
import org.infinity.rpc.appserver.repository.AppRepository;
import org.infinity.rpc.core.server.annotation.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;

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
    public App insert(String name, Boolean enabled, Set<String> authorityNames) {
        App newApp = new App(name, enabled);
        appRepository.save(newApp);
        log.debug("Created Information for app: {}", newApp);
        return newApp;
    }

    @Override
    public void update(String name, Boolean enabled, Set<String> authorityNames) {
        appRepository.findById(name).ifPresent(app -> {
            app.setEnabled(enabled);
            appRepository.save(app);
            log.debug("Updated app: {}", app);
        });
    }

    @Override
    public void deleteById(String id) {
        appRepository.deleteById(id);
    }
}