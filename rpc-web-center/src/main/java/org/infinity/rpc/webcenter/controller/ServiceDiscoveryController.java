package org.infinity.rpc.webcenter.controller;

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
@Slf4j
public class ServiceDiscoveryController {
    @Autowired
    private RegistryService registryService;

    @ApiOperation("获取所有应用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("api/service-discovery/apps")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<String>> findAll() {
        List<String> applications = registryService.getAllApplications();
        return ResponseEntity.ok(applications);
    }

    /**
     * Get all the groups
     *
     * @return groups
     */
    @GetMapping("api/groups")
    public ResponseEntity<List<String>> getAllGroups() {
        List<String> result = registryService.getGroups();
        return ResponseEntity.ok().body(result);
    }

    /**
     * Get providers by group
     *
     * @param group group
     * @return providers
     */
    @GetMapping("api/{group}/providers")
    public ResponseEntity<List<String>> getProvidersByGroup(@PathVariable(value = "group", required = true) String group) {
        List<String> providers = registryService.getProvidersByGroup(group);
        return ResponseEntity.ok().body(providers);
    }


    /**
     * Get nodes by group
     *
     * @param group group
     * @return nodes
     */
    @GetMapping("api/{group}/nodes")
    public ResponseEntity<Map<String, Map<String, List<AddressInfo>>>> getNodesByGroup(@PathVariable(value = "group", required = true) String group) {
        Map<String, Map<String, List<AddressInfo>>> nodes = registryService.getAllNodes(group);
        return ResponseEntity.ok().body(nodes);
    }

    /**
     * Get provider node
     *
     * @param group      group
     * @param provider   provider
     * @param statusNode statusNode
     * @return address info list
     */
    @GetMapping("api/{group}/{provider}/{statusNode}/nodes")
    public ResponseEntity<List<AddressInfo>> getProviderNode(@PathVariable(value = "group", required = true) String group,
                                                             @PathVariable(value = "provider", required = true) String provider,
                                                             @PathVariable(value = "statusNode", required = true) String statusNode) {
        List<AddressInfo> nodes = registryService.getNodes(group, provider, statusNode);
        return ResponseEntity.ok().body(nodes);
    }
}
