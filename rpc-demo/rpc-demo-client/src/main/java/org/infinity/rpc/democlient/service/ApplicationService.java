package org.infinity.rpc.democlient.service;

import org.infinity.rpc.democlient.domain.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {

    Page<Application> find(Pageable pageable, String registryUrl, Boolean active);

}
