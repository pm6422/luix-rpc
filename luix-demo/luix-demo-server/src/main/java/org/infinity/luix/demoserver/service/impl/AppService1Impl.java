package org.infinity.luix.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.common.RpcMethod;
import org.infinity.luix.core.server.annotation.RpcProvider;
import org.infinity.luix.core.server.response.FutureResponse;
import org.infinity.luix.core.server.response.impl.RpcFutureResponse;
import org.infinity.luix.core.server.response.impl.RpcResponse;
import org.infinity.luix.democommon.domain.App;
import org.infinity.luix.democommon.service.AppService;
import org.infinity.luix.demoserver.exception.DataNotFoundException;
import org.infinity.luix.demoserver.repository.AppRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.Resource;
import java.util.Optional;

@RpcProvider(form = "f1", retryCount = "1")
@Slf4j
public class AppService1Impl implements AppService {

    @Resource
    private AppRepository appRepository;

    @Override
    @RpcMethod(retryCount = "2")
    public Page<App> findAll(Pageable pageable) {
        return appRepository.findAll(pageable);
    }

    @Override
    public Optional<App> findById(String id) {
        return appRepository.findById(id);
    }

    @Override
    @Async
    public FutureResponse insert(App domain) {
        appRepository.save(domain);
        log.debug("Created information for app: {}", domain);
        FutureResponse response = new RpcFutureResponse();
        response.onSuccess(RpcResponse.of(domain));
        return response;
    }

    @Override
    public void update(App domain) {
        appRepository.findById(domain.getName()).map(app -> {
            app.setEnabled(domain.getEnabled());
            appRepository.save(app);
            log.debug("Updated app: {}", app);
            return app;
        }).orElseThrow(() -> new DataNotFoundException(domain.getName()));
    }

    @Override
    public void deleteById(String id) {
        appRepository.deleteById(id);
    }
}