package com.luixtech.rpc.demoserver.controller;

import com.luixtech.rpc.democommon.dto.AdminMenuTreeDTO;
import com.luixtech.rpc.democommon.service.AdminMenuService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * REST controller for managing the admin menu.
 */
@RestController
@Slf4j
public class AdminMenuController {

    @Resource
    private AdminMenuService adminMenuService;

    @Operation(summary = "find all menus")
    @GetMapping("/api/admin-menus")
    public ResponseEntity<List<AdminMenuTreeDTO>> find() {
        List<AdminMenuTreeDTO> results = adminMenuService.getMenus();
        return ResponseEntity.ok(results);
    }
}
