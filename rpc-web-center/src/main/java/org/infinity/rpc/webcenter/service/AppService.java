package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.App;

import java.util.Set;

public interface AppService {

    App insert(App domain);

    void update(App domain);

}