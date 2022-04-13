package com.luixtech.luixrpc.democommon.service;

import com.luixtech.luixrpc.democommon.domain.Authority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

public interface AuthorityService {

    List<String> findAllAuthorityNames(Boolean enabled);

    List<String> findAllAuthorityNames();

    Page<Authority> findAll(Pageable pageable);

    List<Authority> findAll();

    List<Authority> find(Query query);

    Optional<Authority> findById(String id);

    void save(Authority authority);

    void deleteById(String id);
}