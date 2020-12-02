package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.Authority;

import java.util.List;

public interface AuthorityService {

    List<String> findAllAuthorityNames(Boolean enabled);

    List<Authority> find(Boolean enabled);

}