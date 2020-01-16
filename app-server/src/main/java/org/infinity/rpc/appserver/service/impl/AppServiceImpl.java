package org.infinity.rpc.appserver.service.impl;

import org.infinity.rpc.appserver.domain.App;
import org.infinity.rpc.appserver.repository.AppRepository;
import org.infinity.rpc.appserver.service.AppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AppServiceImpl implements AppService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppServiceImpl.class);

    @Autowired
    private AppRepository appRepository;

    @Override
    public App insert(String name, Boolean enabled, Set<String> authorityNames) {
        App newApp = new App(name, enabled);
        appRepository.save(newApp);
        LOGGER.debug("Created Information for app: {}", newApp);
        return newApp;
    }

    @Override
    public void update(String name, Boolean enabled, Set<String> authorityNames) {
        appRepository.findById(name).ifPresent(app -> {
            app.setEnabled(enabled);
            appRepository.save(app);
            LOGGER.debug("Updated app: {}", app);
        });
    }
}