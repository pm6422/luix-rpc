package org.infinity.luix.webcenter.service;

import org.infinity.luix.webcenter.domain.AppAuthority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppAuthorityService {

    Page<AppAuthority> find(Pageable pageable, String appName, String authorityName);

}