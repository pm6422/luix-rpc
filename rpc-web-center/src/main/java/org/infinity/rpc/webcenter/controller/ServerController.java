package org.infinity.rpc.webcenter.controller;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class ServerController {
    @Autowired
    private RegistryService registryService;

    /**
     * 获取所有group分组名称
     *
     * @return
     */
    @GetMapping("api/groups")
    public ResponseEntity<List<String>> getAllGroups() {
        List<String> result = registryService.getGroups();
        return ResponseEntity.ok().body(result);
    }
}
