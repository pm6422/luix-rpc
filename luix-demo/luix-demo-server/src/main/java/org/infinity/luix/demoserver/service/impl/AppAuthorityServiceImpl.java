package org.infinity.luix.demoserver.service.impl;

import org.infinity.luix.core.server.annotation.RpcProvider;
import org.infinity.luix.democommon.domain.AppAuthority;
import org.infinity.luix.democommon.service.AppAuthorityService;
import org.infinity.luix.demoserver.repository.AppAuthorityRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Resource;

@RpcProvider
public class AppAuthorityServiceImpl implements AppAuthorityService {

    @Resource
    private AppAuthorityRepository appAuthorityRepository;

    @Override
    public Page<AppAuthority> find(Pageable pageable, String appName, String authorityName) {
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Example<AppAuthority> queryExample = Example.of(new AppAuthority(appName, authorityName), matcher);
        return appAuthorityRepository.findAll(queryExample, pageable);
    }
}