package org.infinity.rpc.webcenter.controller;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class ServerController {
    @Autowired
    private RegistryService registryService;

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
    public ResponseEntity<List<String>> getProvidersByGroup(@PathVariable("group") String group) {
        if (StringUtils.isEmpty(group)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        List<String> providers = registryService.getProvidersByGroup(group);
        return ResponseEntity.ok().body(providers);
    }
}
