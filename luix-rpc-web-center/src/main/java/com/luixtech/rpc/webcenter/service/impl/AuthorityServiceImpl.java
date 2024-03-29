package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.webcenter.domain.Authority;
import com.luixtech.rpc.webcenter.repository.AuthorityRepository;
import com.luixtech.rpc.webcenter.service.AuthorityService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AuthorityServiceImpl implements AuthorityService {
    private final AuthorityRepository authorityRepository;

    @Override
    public List<String> findAllAuthorityNames(Boolean enabled) {
        return authorityRepository.findByEnabled(enabled).stream().map(Authority::getName)
                .collect(Collectors.toList());
    }

    @Override
    public List<Authority> find(Boolean enabled) {
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Example<Authority> queryExample = Example.of(new Authority(enabled), matcher);
        return authorityRepository.findAll(queryExample);
    }
}