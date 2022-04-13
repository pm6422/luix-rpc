package com.luixtech.luixrpc.demoserver.service.impl;

import com.luixtech.luixrpc.core.server.annotation.RpcProvider;
import com.luixtech.luixrpc.democommon.domain.Authority;
import com.luixtech.luixrpc.democommon.service.AuthorityService;
import com.luixtech.luixrpc.demoserver.repository.AuthorityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RpcProvider
public class AuthorityServiceImpl implements AuthorityService {

    @Resource
    private AuthorityRepository authorityRepository;
    @Resource
    private MongoTemplate       mongoTemplate;

    @Override
    public List<String> findAllAuthorityNames(Boolean enabled) {
        return authorityRepository.findByEnabled(enabled).stream().map(Authority::getName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findAllAuthorityNames() {
        return authorityRepository.findAll().stream().map(Authority::getName)
                .collect(Collectors.toList());
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
    public List<Authority> find(Query query) {
        return mongoTemplate.find(query, Authority.class);
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