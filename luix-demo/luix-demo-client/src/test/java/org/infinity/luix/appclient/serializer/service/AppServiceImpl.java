package org.infinity.luix.appclient.serializer.service;

import org.infinity.luix.core.server.response.FutureResponse;
import org.infinity.luix.democommon.domain.App;
import org.infinity.luix.democommon.service.AppService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class AppServiceImpl implements AppService {
    @Override
    public Page<App> findAll(Pageable pageable) {
        List<App> list = new ArrayList<>(100);
        IntStream.range(0, 100).forEach(i -> list.add(new App("testApp", true)));
        return new PageImpl<>(list, pageable, 100);
    }

    @Override
    public Optional<App> findById(String id) {
        return Optional.empty();
    }

    @Override
    public FutureResponse insert(App domain) {
        return null;
    }

    @Override
    public void update(App domain) {

    }

    @Override
    public void deleteById(String id) {

    }
}
