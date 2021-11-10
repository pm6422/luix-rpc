package org.infinity.luix.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.webcenter.config.ApplicationConstants;
import org.infinity.luix.webcenter.repository.RpcApplicationRepository;
import org.infinity.luix.webcenter.service.RpcApplicationService;
import org.infinity.luix.webcenter.service.RpcConsumerService;
import org.infinity.luix.webcenter.service.RpcProviderService;
import org.infinity.luix.webcenter.utils.HttpHeaderUtils;
import org.infinity.luix.webcenter.domain.RpcApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

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
            @ApiParam(value = "registry url identity", required = true, defaultValue = ApplicationConstants.DEFAULT_REG)
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
            @ApiParam(value = "registry url identity", required = true, defaultValue = ApplicationConstants.DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "application name(fuzzy query)") @RequestParam(value = "name", required = false) String name,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcApplication> results = rpcApplicationService.find(pageable, registryIdentity, name, active);
        if (!results.isEmpty()) {
            results.getContent().forEach(domain -> {
                if (rpcProviderService.existsApplication(registryIdentity, domain.getName(), true)) {
                    domain.setProviding(true);
                    domain.setActive(true);
                }
                if (rpcConsumerService.existsApplication(registryIdentity, domain.getName(), true)) {
                    domain.setConsuming(true);
                    domain.setActive(true);
                }
            });
        }
        return ResponseEntity.ok().headers(HttpHeaderUtils.generatePageHeaders(results)).body(results.getContent());
    }
}
