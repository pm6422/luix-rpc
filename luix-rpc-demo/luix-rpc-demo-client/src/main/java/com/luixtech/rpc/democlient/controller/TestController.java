package com.luixtech.rpc.democlient.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.rpc.core.client.annotation.RpcConsumer;
import com.luixtech.rpc.democommon.domain.App;
import com.luixtech.rpc.democommon.domain.Authority;
import com.luixtech.rpc.democommon.service.AppService;
import com.luixtech.rpc.democommon.service.AuthorityService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.luixtech.rpc.democommon.domain.Authority.ADMIN;
import static com.luixtech.rpc.democommon.domain.Authority.USER;

@RestController
@Slf4j
public class TestController {

    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private Environment        env;
    @RpcConsumer
    private AuthorityService   authorityService;
    @RpcConsumer(providerAddresses = "${application.url.appServiceProviderUrl}", form = "f2")
    private AppService         appService;

    @ApiOperation("test kryo serialization and deserialization")
    @GetMapping("/api/authority-names")
    public ResponseEntity<List<String>> find() {
        Query query = Query.query(Criteria.where("name").in(ADMIN, USER));
        List<String> authorities = authorityService.find(query).stream().map(Authority::getName).collect(Collectors.toList());
        return ResponseEntity.ok(authorities);
    }

    @ApiOperation("direct connect")
    @GetMapping("/api/tests/direct-url")
    public List<App> directUrl() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<App> all = appService.findAll(pageable);
        return all.getContent();
    }
}
