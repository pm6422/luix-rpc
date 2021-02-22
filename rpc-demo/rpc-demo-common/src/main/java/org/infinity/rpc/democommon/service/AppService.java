package org.infinity.rpc.democommon.service;

import org.infinity.rpc.core.server.response.FutureResponse;
import org.infinity.rpc.democommon.domain.App;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AppService {

    Page<App> findAll(Pageable pageable);

    Optional<App> findById(String id);

    FutureResponse insert(App domain);

    void update(App domain);

    void deleteById(String id);
}