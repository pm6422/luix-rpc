package org.infinity.rpc.demoserver.service.impl;

import org.infinity.rpc.core.server.annotation.RpcProvider;
import org.infinity.rpc.democommon.domain.AppAuthority;
import org.infinity.rpc.democommon.service.AppAuthorityService;
import org.infinity.rpc.demoserver.repository.AppAuthorityRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RpcProvider
public class AppAuthorityServiceImpl implements AppAuthorityService {

    private final AppAuthorityRepository appAuthorityRepository;

    public AppAuthorityServiceImpl(AppAuthorityRepository appAuthorityRepository) {
        this.appAuthorityRepository = appAuthorityRepository;
    }

    @Override
    public Page<AppAuthority> find(Pageable pageable, String appName, String authorityName) {
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Example<AppAuthority> queryExample = Example.of(new AppAuthority(appName, authorityName), matcher);
        return appAuthorityRepository.findAll(queryExample, pageable);
    }
}