package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.App;

public interface AppService {

    App insert(App domain);

    void update(App domain);

}