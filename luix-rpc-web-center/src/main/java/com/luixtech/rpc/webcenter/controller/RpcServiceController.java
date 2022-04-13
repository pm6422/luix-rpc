package com.luixtech.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.rpc.webcenter.component.HttpHeaderCreator;
import com.luixtech.rpc.webcenter.domain.RpcService;
import com.luixtech.rpc.webcenter.dto.InterfaceActivateDTO;
import com.luixtech.rpc.webcenter.dto.ProviderActivateDTO;
import com.luixtech.rpc.webcenter.exception.DataNotFoundException;
import com.luixtech.rpc.webcenter.repository.RpcServiceRepository;
import com.luixtech.rpc.webcenter.service.RpcConsumerService;
import com.luixtech.rpc.webcenter.service.RpcProviderService;
import com.luixtech.rpc.webcenter.service.RpcServiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.luixtech.rpc.webcenter.config.ApplicationConstants.DEFAULT_REG;
import static com.luixtech.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class RpcServiceController {

    @Resource
    private RpcServiceService     rpcServiceService;
    @Resource
    private RpcServiceRepository  rpcServiceRepository;
    @Resource
    private RpcProviderService    rpcProviderService;
    @Resource
    private RpcConsumerService    rpcConsumerService;
    @Resource
    private RpcProviderController rpcProviderController;
    @Resource
    private HttpHeaderCreator     httpHeaderCreator;

    @ApiOperation("find service by ID")
    @GetMapping("/api/rpc-services/{id}")
    @Timed
    public ResponseEntity<RpcService> findById(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        RpcService domain = rpcServiceRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        if (rpcProviderService.existsService(domain.getRegistryIdentity(), domain.getInterfaceName(), true)) {
            domain.setProviding(true);
            domain.setActive(true);
        }
        if (rpcConsumerService.existsService(domain.getRegistryIdentity(), domain.getInterfaceName(), true)) {
            domain.setConsuming(true);
            domain.setActive(true);
        }
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("find service")
    @GetMapping("/api/rpc-services/service")
    @Timed
    public ResponseEntity<RpcService> find(
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        String id = DigestUtils.md5DigestAsHex((interfaceName + "@" + registryIdentity).getBytes());
        RpcService domain = rpcServiceRepository.findById(id).orElseThrow(() -> new DataNotFoundException(interfaceName));
        if (rpcProviderService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
            domain.setProviding(true);
            domain.setActive(true);
        }
        if (rpcConsumerService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
            domain.setConsuming(true);
            domain.setActive(true);
        }
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("find service list")
    @GetMapping("/api/rpc-services")
    @Timed
    public ResponseEntity<List<RpcService>> findRpcServices(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        Page<RpcService> results = rpcServiceService.find(pageable, registryIdentity, interfaceName);
        if (!results.isEmpty()) {
            results.getContent().forEach(domain -> {
                if (rpcProviderService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
                    domain.setProviding(true);
                    domain.setActive(true);
                }
                if (rpcConsumerService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
                    domain.setConsuming(true);
                    domain.setActive(true);
                }
            });
        }
        return ResponseEntity.ok().headers(generatePageHeaders(results)).body(results.getContent());
    }

    @ApiOperation("activate all providers of the interface")
    @PutMapping("/api/rpc-services/activate")
    public ResponseEntity<Void> activate(@Valid @RequestBody InterfaceActivateDTO activateDTO) {
        rpcProviderService.find(activateDTO.getRegistryIdentity(), activateDTO.getInterfaceName(), false)
                .forEach(provider -> rpcProviderController.activate(
                        ProviderActivateDTO.builder()
                                .registryIdentity(activateDTO.getRegistryIdentity())
                                .providerUrl(provider.getUrl())
                                .build()));
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }

    @ApiOperation("deactivate all providers of the interface")
    @PutMapping("/api/rpc-services/deactivate")
    @Timed
    public ResponseEntity<Void> deactivate(@Valid @RequestBody InterfaceActivateDTO activateDTO) {
        rpcProviderService.find(activateDTO.getRegistryIdentity(), activateDTO.getInterfaceName(), true)
                .forEach(provider -> rpcProviderController.deactivate(
                        ProviderActivateDTO.builder()
                                .registryIdentity(activateDTO.getRegistryIdentity())
                                .providerUrl(provider.getUrl())
                                .build()));
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }
}
