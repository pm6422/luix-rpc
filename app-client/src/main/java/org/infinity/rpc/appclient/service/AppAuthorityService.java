package org.infinity.rpc.appclient.service;

import org.infinity.rpc.appclient.domain.AppAuthority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppAuthorityService {

    Page<AppAuthority> findByAppNameAndAuthorityNameCombinations(Pageable pageable, String appName,
                                                                 String authorityName);

}