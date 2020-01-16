package org.infinity.rpc.appclient.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.infinity.rpc.appclient.entity.MenuTreeNode;
import org.infinity.rpc.appclient.service.AdminMenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger                       LOGGER = LoggerFactory.getLogger(AdminMenuController.class);
    @Autowired
    private              AdminMenuService             adminMenuService;

    @ApiOperation("查询菜单")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("/api/admin-menu/menus")
    @Timed
    public ResponseEntity<List<MenuTreeNode>> menus() {
        List<MenuTreeNode> results = adminMenuService.getMenus();
        return ResponseEntity.ok(results);
    }
}
