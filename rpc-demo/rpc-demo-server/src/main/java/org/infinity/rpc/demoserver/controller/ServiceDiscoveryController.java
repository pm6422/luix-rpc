package org.infinity.rpc.demoserver.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.demoserver.dto.ProviderDTO;
import org.infinity.rpc.demoserver.dto.RegistryDTO;
import org.infinity.rpc.demoserver.service.RegistryService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_OK;

@RestController
@Api(tags = "服务发现")
@Slf4j
public class ServiceDiscoveryController {

    private final InfinityProperties infinityProperties;
    private final RegistryService    registryService;

    public ServiceDiscoveryController(InfinityProperties infinityProperties, RegistryService registryService) {
        this.infinityProperties = infinityProperties;
        this.registryService = registryService;
    }

    @ApiOperation("获取所有注册中心")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("api/service-discovery/registries")
    public ResponseEntity<List<RegistryDTO>> findRegistries() {
        return ResponseEntity.ok(registryService.getRegistries());
    }

    @ApiOperation("获取所有应用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("api/service-discovery/apps")
    public ResponseEntity<List<ApplicationExtConfig>> findApps(@ApiParam(value = "URL", required = true)
                                                               @RequestParam(value = "url") String url) {
//        return ResponseEntity.ok(registryService.getAllApps());
        return null;
    }

    @ApiOperation("获取所有服务提供者分组")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("api/service-discovery/provider/groups")
    public ResponseEntity<List<String>> findProviderGroups(@ApiParam(value = "URL", required = true)
                                                           @RequestParam(value = "url") String url) {
        Registry registry = registryService.findRegistry(url);
        return ResponseEntity.ok(registry.getAllProviderForms());
    }

    @ApiOperation("获取所有服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("api/service-discovery/provider/providers")
    public ResponseEntity<List<ProviderDTO>> findProviders(@ApiParam(value = "URL", required = true)
                                                           @RequestParam(value = "url") String url,
                                                           @ApiParam(value = "服务提供者分组", required = true)
                                                           @RequestParam(value = "group") String group) {
        Registry registry = registryService.findRegistry(url);
//        Map<String, Map<String, List<AddressInfo>>> nodeMap = registry.getAllProviders(group);
        List<ProviderDTO> providers = new ArrayList<>();
//        if (MapUtils.isNotEmpty(nodeMap)) {
//            for (Map.Entry<String, Map<String, List<AddressInfo>>> entry : nodeMap.entrySet()) {
//                List<AddressInfo> activeProviders = entry.getValue().get(ZookeeperStatusNode.ACTIVE.getValue());
//                List<AddressInfo> inactiveProviders = entry.getValue().get(ZookeeperStatusNode.INACTIVE.getValue());
//                providers.add(ProviderDTO.of(entry.getKey(), activeProviders, inactiveProviders));
//            }
//        }
        return ResponseEntity.ok(providers);
    }

    @ApiOperation("启用服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功执行")})
    @PostMapping("/api/service-discovery/provider/enable")
    public ResponseEntity<Void> activate(@RequestBody String providerUrl) {
        infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().activate(Url.valueOf(providerUrl)));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ApiOperation("禁用服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功执行")})
    @PostMapping("/api/service-discovery/provider/disable")
    public ResponseEntity<Void> deactivate(@RequestBody String providerUrl) {
        infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().deactivate(Url.valueOf(providerUrl)));
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
