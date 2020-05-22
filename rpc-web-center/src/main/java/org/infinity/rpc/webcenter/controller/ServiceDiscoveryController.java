package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.registry.zookeeper.utils.AddressInfo;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_OK;

@RestController
@Api(tags = "服务发现")
@Slf4j
public class ServiceDiscoveryController {
    @Autowired
    private RegistryService registryService;

    @ApiOperation("获取所有应用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("api/service-discovery/apps")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<String>> findApps() {
        List<String> applications = registryService.getAllApplications();
        return ResponseEntity.ok(applications);
    }

    @ApiOperation("获取所有服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("api/service-discovery/providers")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Map<String, Map<String, List<AddressInfo>>>> findProviders() {
        Map<String, Map<String, List<AddressInfo>>> nodes = registryService.getAllNodes("provider");
        return ResponseEntity.ok().body(nodes);
    }

    /**
     * Get provider node
     *
     * @param statusNode statusNode
     * @return address info list
     */
    @GetMapping("api/service-discovery/{providerName}/{statusNode}/nodes")
    public ResponseEntity<List<AddressInfo>> getProviderNode(@PathVariable(value = "providerName", required = true) String providerName,
                                                             @PathVariable(value = "statusNode", required = true) String statusNode) {
        List<AddressInfo> nodes = registryService.getNodes("provider", providerName, statusNode);
        return ResponseEntity.ok().body(nodes);
    }

    /**
     * Get all the groups
     *
     * @return groups
     */
//    @GetMapping("api/service-discovery/groups")
//    public ResponseEntity<List<String>> getAllGroups() {
//        List<String> result = registryService.getGroups();
//        return ResponseEntity.ok().body(result);
//    }

//    /**
//     * Get providers
//     *
//     * @param group group
//     * @return providers
//     */
//    @GetMapping("api/service-discovery/{group}/providers")
//    public ResponseEntity<List<String>> getProvidersByGroup(@PathVariable(value = "group", required = true) String group) {
//        List<String> providers = registryService.getProvidersByGroup(group);
//        return ResponseEntity.ok().body(providers);
//    }
}
