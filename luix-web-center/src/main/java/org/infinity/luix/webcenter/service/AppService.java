package org.infinity.luix.webcenter.service;

import org.infinity.luix.webcenter.domain.App;

public interface AppService {

    App insert(App domain);

    void update(App domain);

}