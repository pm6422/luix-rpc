package org.infinity.rpc.appclient.service.impl;

import org.infinity.rpc.appclient.domain.Authority;
import org.infinity.rpc.appclient.repository.AuthorityRepository;
import org.infinity.rpc.appclient.service.AuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthorityServiceImpl implements AuthorityService {

    @Autowired
    private AuthorityRepository authorityRepository;

    @Override
    public List<String> findAllAuthorityNames(Boolean enabled) {
        List<String> results = authorityRepository.findByEnabled(enabled).stream().map(Authority::getName)
                .collect(Collectors.toList());
        return results;
    }

    @Override
    public List<String> findAllAuthorityNames() {
        List<String> results = authorityRepository.findAll().stream().map(Authority::getName)
                .collect(Collectors.toList());
        return results;
    }
}