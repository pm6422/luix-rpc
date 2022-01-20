package org.infinity.luix.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.luix.webcenter.component.HttpHeaderCreator;
import org.infinity.luix.webcenter.domain.AdminMenu;
import org.infinity.luix.webcenter.dto.AdminAuthorityMenusDTO;
import org.infinity.luix.webcenter.dto.AdminMenuTreeDTO;
import org.infinity.luix.webcenter.repository.AdminMenuRepository;
import org.infinity.luix.webcenter.repository.AuthorityAdminMenuRepository;
import org.infinity.luix.webcenter.service.AdminMenuService;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.domain.AuthorityAdminMenu;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for managing the authority admin menu.
 */
@RestController
@Slf4j
public class AuthorityAdminMenuController {

    @Resource
    private AuthorityAdminMenuRepository authorityAdminMenuRepository;
    @Resource
    private AdminMenuRepository          adminMenuRepository;
    @Resource
    private AdminMenuService             adminMenuService;
    @Resource
    private HttpHeaderCreator            httpHeaderCreator;

    @ApiOperation("find menu tree by authority name")
    @GetMapping("/api/authority-admin-menus")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<AdminMenuTreeDTO>> findAuthorityMenus(
            @ApiParam(value = "application name", required = true) @RequestParam(value = "appName") String appName,
            @ApiParam(value = "authority name", required = true) @RequestParam(value = "authorityName") String authorityName) {
        List<AdminMenuTreeDTO> results = adminMenuService.getAuthorityMenus(appName, authorityName);
        return ResponseEntity.ok(results);
    }

    @ApiOperation("update authority menu")
    @PutMapping("/api/authority-admin-menus")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> update(
            @ApiParam(value = "new authority menu", required = true) @Valid @RequestBody AdminAuthorityMenusDTO dto) {
        log.debug("REST request to update admin authority menus: {}", dto);
        // 删除当前权限下的所有菜单
        Set<String> appAdminMenuIds = adminMenuRepository.findByAppName(dto.getAppName()).stream().map(AdminMenu::getId)
                .collect(Collectors.toSet());
        authorityAdminMenuRepository.deleteByAuthorityNameAndAdminMenuIdIn(dto.getAuthorityName(),
                new ArrayList<>(appAdminMenuIds));

        // 构建权限映射集合
        if (CollectionUtils.isNotEmpty(dto.getAdminMenuIds())) {
            List<AuthorityAdminMenu> adminAuthorityMenus = dto.getAdminMenuIds().stream()
                    .map(adminMenuId -> new AuthorityAdminMenu(dto.getAuthorityName(), adminMenuId))
                    .collect(Collectors.toList());
            // 批量插入
            authorityAdminMenuRepository.saveAll(adminAuthorityMenus);
        }
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }

    @ApiOperation("find menu list associated with current user")
    @GetMapping("/api/authority-admin-menus/user-links")
    public ResponseEntity<List<AdminMenu>> findUserAuthorityLinks(
            @ApiParam(value = "application name", required = true) @RequestParam(value = "appName") String appName) {
        List<AdminMenu> results = adminMenuService.getUserAuthorityLinks(appName);
        return ResponseEntity.ok(results);
    }

    @ApiOperation("find menu tree associated with current user")
    @GetMapping("/api/authority-admin-menus/user-menus")
    public ResponseEntity<List<AdminMenuTreeDTO>> findUserAuthorityMenus(
            @ApiParam(value = "application name", required = true) @RequestParam(value = "appName") String appName) {
        List<AdminMenuTreeDTO> results = adminMenuService.getUserAuthorityMenus(appName);
        return ResponseEntity.ok(results);
    }
}
