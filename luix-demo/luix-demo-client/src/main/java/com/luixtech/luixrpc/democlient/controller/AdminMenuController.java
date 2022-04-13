package com.luixtech.luixrpc.democlient.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.client.annotation.RpcConsumer;
import com.luixtech.luixrpc.democommon.dto.AdminMenuTreeDTO;
import com.luixtech.luixrpc.democommon.service.AdminMenuService;
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

    @RpcConsumer(requestTimeout = "10000")
    public void setAdminMenuService(AdminMenuService adminMenuService) {
        this.adminMenuService = adminMenuService;
    }

    @ApiOperation("find all menus")
    @GetMapping("/api/admin-menus")
    public ResponseEntity<List<AdminMenuTreeDTO>> find() {
        List<AdminMenuTreeDTO> results = adminMenuService.getMenus();
        return ResponseEntity.ok(results);
    }
}
