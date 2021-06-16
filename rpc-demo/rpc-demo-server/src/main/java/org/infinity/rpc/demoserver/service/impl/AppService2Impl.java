package org.infinity.rpc.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.server.annotation.RpcProvider;
import org.infinity.rpc.core.server.response.FutureResponse;
import org.infinity.rpc.core.server.response.impl.RpcFutureResponse;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.democommon.domain.App;
import org.infinity.rpc.democommon.service.AppService;
import org.infinity.rpc.demoserver.exception.NoDataFoundException;
import org.infinity.rpc.demoserver.repository.AppRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Resource;
import java.util.Optional;

@RpcProvider(form = "f2", retryCount = "1")
@Slf4j
public class AppService2Impl implements AppService {

    @Resource
    private AppRepository appRepository;

    @Override
    public Page<App> findAll(Pageable pageable) {
        return appRepository.findAll(pageable);
    }

    @Override
    public Optional<App> findById(String id) {
        return appRepository.findById(id);
    }

    @Override
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
        }).orElseThrow(() -> new NoDataFoundException(domain.getName()));
    }

    @Override
    public void deleteById(String id) {
        appRepository.deleteById(id);
    }
}