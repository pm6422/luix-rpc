package org.infinity.rpc.webcenter.service.impl;

import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.repository.AuthorityRepository;
import org.infinity.rpc.webcenter.service.AuthorityService;
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