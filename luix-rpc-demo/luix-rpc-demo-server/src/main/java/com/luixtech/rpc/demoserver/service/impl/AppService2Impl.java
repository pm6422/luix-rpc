package com.luixtech.rpc.demoserver.service.impl;

import com.luixtech.rpc.core.server.annotation.RpcProvider;
import com.luixtech.rpc.core.server.response.FutureResponse;
import com.luixtech.rpc.core.server.response.impl.RpcFutureResponse;
import com.luixtech.rpc.core.server.response.impl.RpcResponse;
import com.luixtech.rpc.democommon.domain.App;
import com.luixtech.rpc.democommon.service.AppService;
import com.luixtech.rpc.demoserver.exception.DataNotFoundException;
import com.luixtech.rpc.demoserver.repository.AppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Resource;

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
    public App findById(String id) {
        return appRepository.findById(id).orElse(null);
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
        }).orElseThrow(() -> new DataNotFoundException(domain.getName()));
    }

    @Override
    public void deleteById(String id) {
        appRepository.deleteById(id);
    }
}