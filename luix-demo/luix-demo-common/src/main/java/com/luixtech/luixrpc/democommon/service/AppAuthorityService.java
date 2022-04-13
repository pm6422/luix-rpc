package com.luixtech.luixrpc.democommon.service;

import com.luixtech.luixrpc.democommon.domain.AppAuthority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppAuthorityService {

    Page<AppAuthority> find(Pageable pageable, String appName, String authorityName);

}