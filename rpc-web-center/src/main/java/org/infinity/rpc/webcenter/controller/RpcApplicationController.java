package org.infinity.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.infinity.rpc.webcenter.repository.RpcApplicationRepository;
import org.infinity.rpc.webcenter.service.RpcApplicationService;
import org.infinity.rpc.webcenter.service.RpcConsumerService;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static org.infinity.rpc.webcenter.config.ApplicationConstants.DEFAULT_REG;
import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class RpcApplicationController {

    @Resource
    private RpcApplicationRepository rpcApplicationRepository;
    @Resource
    private RpcApplicationService    rpcApplicationService;
    @Resource
    private RpcProviderService       rpcProviderService;
    @Resource
    private RpcConsumerService       rpcConsumerService;

    @ApiOperation("find all application names")
    @GetMapping("api/rpc-applications/names")
    @Timed
    public ResponseEntity<List<String>> findApplications(
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity) {
        List<String> results = rpcApplicationRepository.findByRegistryIdentity(registryIdentity)
                .stream().map(RpcApplication::getName).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @ApiOperation("find application list")
    @GetMapping("api/rpc-applications")
    @Timed
    public ResponseEntity<List<RpcApplication>> findApplications(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "application name(fuzzy query)") @RequestParam(value = "name", required = false) String name,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcApplication> results = rpcApplicationService.find(pageable, registryIdentity, name, active);
        if (!results.isEmpty()) {
            results.getContent().forEach(domain -> {
                if (rpcProviderService.existsApplication(registryIdentity, domain.getName(), true)) {
                    domain.setProviding(true);
                }
                if (rpcConsumerService.existsApplication(registryIdentity, domain.getName(), true)) {
                    domain.setConsuming(true);
                }
                if (domain.isProviding() || domain.isConsuming()) {
                    domain.setActive(true);
                } else {
                    domain.setActive(false);
                }
            });
        }
        return ResponseEntity.ok().headers(generatePageHeaders(results)).body(results.getContent());
    }
}
