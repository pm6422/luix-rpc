package com.luixtech.rpc.webcenter.service;

import com.luixtech.rpc.webcenter.domain.Authority;

import java.util.List;

public interface AuthorityService {

    List<String> findAllAuthorityNames(Boolean enabled);

    List<Authority> find(Boolean enabled);

}