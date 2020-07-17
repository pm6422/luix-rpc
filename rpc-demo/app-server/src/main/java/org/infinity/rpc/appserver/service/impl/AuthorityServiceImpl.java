package org.infinity.rpc.appserver.service.impl;

import org.infinity.app.common.domain.Authority;
import org.infinity.app.common.service.AuthorityService;
import org.infinity.rpc.appserver.repository.AuthorityRepository;
import org.infinity.rpc.core.server.annotation.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Provider
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

    @Override
    public Page<Authority> findAll(Pageable pageable) {
        return authorityRepository.findAll(pageable);
    }

    @Override
    public List<Authority> findAll() {
        return authorityRepository.findAll();
    }

    @Override
    public Optional<Authority> findById(String id) {
        return authorityRepository.findById(id);
    }

    @Override
    public void save(Authority authority) {
        authorityRepository.save(authority);
    }

    @Override
    public void deleteById(String id) {
        authorityRepository.deleteById(id);
    }
}