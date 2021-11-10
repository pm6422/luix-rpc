package org.infinity.luix.democlient.restservice;

import com.dtflys.forest.annotation.BaseRequest;
import com.dtflys.forest.annotation.JSONBody;
import com.dtflys.forest.annotation.Post;
import org.infinity.luix.democommon.domain.App;

/**
 * http://forest.dtflyx.com/docs
 */
@BaseRequest(baseURL = "${rpcDemoServerAddress}")
public interface AppRestService {

    /**
     * Create application
     *
     * @param domain application
     */
    @Post(url = "/api/apps", timeout = 200)
    void create(@JSONBody App domain);
}
