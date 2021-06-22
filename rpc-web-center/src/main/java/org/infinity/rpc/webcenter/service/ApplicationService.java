package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.domain.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {

    Page<Application> find(Pageable pageable, String registryUrl, String name, Boolean active);

    Application remoteQueryApplication(Url registryUrl, Url url);

    void inactivate(String applicationName, String registryIdentity);
}
