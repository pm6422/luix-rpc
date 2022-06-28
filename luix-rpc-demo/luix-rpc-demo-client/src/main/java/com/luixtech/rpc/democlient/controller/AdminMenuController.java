package com.luixtech.rpc.democlient.controller;

import com.luixtech.rpc.core.client.annotation.RpcConsumer;
import com.luixtech.rpc.democommon.domain.AdminMenu;
import com.luixtech.rpc.democommon.dto.AdminMenuDTO;
import com.luixtech.rpc.democommon.service.AdminMenuService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
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

    @Operation(summary = "find all menus")
    @GetMapping("/api/admin-menus")
    public ResponseEntity<List<AdminMenuDTO>> find() {
        List<AdminMenuDTO> results = adminMenuService.getMenus();
        return ResponseEntity.ok(results);
    }
}
