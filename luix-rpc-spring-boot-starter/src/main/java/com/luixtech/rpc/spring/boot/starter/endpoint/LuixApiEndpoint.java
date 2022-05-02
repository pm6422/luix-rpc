package com.luixtech.rpc.spring.boot.starter.endpoint;

import com.google.common.collect.Maps;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


@WebEndpoint(id = "luix")
public class LuixApiEndpoint {

    /**
     * GET /management/luix
     *
     * @return a Map with a String defining a category of openApi as Key and
     * another Map containing openApi related to this category as Value
     */
    @ReadOperation
    public List<Map<String, String>> luixApi() {
        Map<String, String> openApi = Maps.newHashMap();
        openApi.put("providers-count", "100");
        openApi.put("consumers-count", "100");
        return Arrays.asList(openApi);
    }
}