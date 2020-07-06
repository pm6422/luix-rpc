package org.infinity.rpc.appclient.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.infinity.app.common.entity.MenuTreeNode;
import org.infinity.app.common.service.AdminMenuService;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * REST controller for managing the admin menu.
 */
@RestController
@Api(tags = "管理菜单")
public class AdminMenuController {

    private static final Logger           LOGGER = LoggerFactory.getLogger(AdminMenuController.class);
    @Consumer(timeout = 10000)
    private              AdminMenuService adminMenuService;

//    @Consumer(timeout = 10000)
//    public void setAdminMenuService(AdminMenuService adminMenuService) {
//        this.adminMenuService = adminMenuService;
//    }

    @ApiOperation("查询菜单")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("/api/admin-menu/menus")
    public ResponseEntity<List<MenuTreeNode>> menus() {
        List<MenuTreeNode> results = adminMenuService.getMenus();
        return ResponseEntity.ok(results);
    }
}
