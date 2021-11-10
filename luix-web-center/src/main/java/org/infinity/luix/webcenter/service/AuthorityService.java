package org.infinity.luix.webcenter.service;

import org.infinity.luix.webcenter.domain.Authority;

import java.util.List;

public interface AuthorityService {

    List<String> findAllAuthorityNames(Boolean enabled);

    List<Authority> find(Boolean enabled);

}