package org.infinity.app.common.service;

import org.infinity.app.common.domain.Authority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AuthorityService {

    List<String> findAllAuthorityNames(Boolean enabled);

    List<String> findAllAuthorityNames();

    Page<Authority> findAll(Pageable pageable);

    List<Authority> findAll();

    Optional<Authority> findById(String id);

    void save(Authority authority);

    void deleteById(String id);
}