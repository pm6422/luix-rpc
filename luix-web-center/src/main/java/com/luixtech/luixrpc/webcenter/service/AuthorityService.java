package com.luixtech.luixrpc.webcenter.service;

import com.luixtech.luixrpc.webcenter.domain.Authority;

import java.util.List;

public interface AuthorityService {

    List<String> findAllAuthorityNames(Boolean enabled);

    List<Authority> find(Boolean enabled);

}