package org.infinity.rpc.democlient.restservice;

import com.dtflys.forest.annotation.BaseRequest;
import com.dtflys.forest.annotation.JSONBody;
import com.dtflys.forest.annotation.Post;
import org.infinity.rpc.democommon.domain.App;

/**
 * http://forest.dtflyx.com/docs
 */
@BaseRequest(baseURL = "${rpcDemoServerAddress}")
public interface AppRestService {

    @Post(url = "/api/app/apps", timeout = 200)
    void insert(@JSONBody App domain);
}
