package com.luixtech.luixrpc.democommon.service;

import com.luixtech.luixrpc.core.server.response.FutureResponse;
import com.luixtech.luixrpc.democommon.domain.App;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppService {

    Page<App> findAll(Pageable pageable);

//    Optional<App> findById(String id);

    App findById(String id);

    FutureResponse insert(App domain);

    void update(App domain);

    void deleteById(String id);
}