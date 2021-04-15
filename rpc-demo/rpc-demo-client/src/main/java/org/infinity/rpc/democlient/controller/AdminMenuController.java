package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.democommon.dto.AdminMenuTreeDTO;
import org.infinity.rpc.democommon.service.AdminMenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing the admin menu.
 */
@RestController
@Slf4j
public class AdminMenuController {

    private AdminMenuService adminMenuService;

    @Consumer(requestTimeout = 10000)
    public void setAdminMenuService(AdminMenuService adminMenuService) {
        this.adminMenuService = adminMenuService;
    }

    @ApiOperation("find all menus")
    @GetMapping("/api/admin-menu/menus")
    public ResponseEntity<List<AdminMenuTreeDTO>> find() {
        List<AdminMenuTreeDTO> results = adminMenuService.getMenus();
        return ResponseEntity.ok(results);
    }
}
