package org.infinity.rpc.democommon.service;

import org.infinity.rpc.democommon.domain.App;
import org.infinity.rpc.utilities.annotation.AsyncMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AppService {

    Page<App> findAll(Pageable pageable);

    Optional<App> findById(String id);

    @AsyncMethod
    App insert(App domain);

    void update(App domain);

    void deleteById(String id);
}