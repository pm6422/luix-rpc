package com.luixtech.rpc.democommon.service;

import com.luixtech.rpc.democommon.domain.AppAuthority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppAuthorityService {

    Page<AppAuthority> find(Pageable pageable, String appName, String authorityName);

}