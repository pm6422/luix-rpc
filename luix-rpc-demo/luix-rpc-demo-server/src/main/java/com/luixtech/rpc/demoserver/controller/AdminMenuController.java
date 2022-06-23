package com.luixtech.rpc.demoserver.controller;

import com.luixtech.rpc.democommon.domain.AdminMenu;
import com.luixtech.rpc.democommon.dto.AdminMenuTreeDTO;
import com.luixtech.rpc.democommon.service.AdminMenuService;
import com.luixtech.rpc.demoserver.repository.AdminMenuQueryRepository;
import com.turkraft.springfilter.boot.Filter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private AdminMenuQueryRepository adminMenuQueryRepository;
    @Resource
    private AdminMenuService         adminMenuService;

    @Operation(summary = "find all menus")
    @GetMapping("/api/admin-menus")
    public ResponseEntity<List<AdminMenuTreeDTO>> find() {
        List<AdminMenuTreeDTO> results = adminMenuService.getMenus();
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "advanced query")
    @GetMapping(value = "/api/admin-menus/query")
    @Parameter(in = ParameterIn.QUERY, name = "filter", description = "query criteria",
            schema = @Schema(type = "string", defaultValue = "(code:'user-authority') or (name:'Authority')"))
    public Page<AdminMenu> query(@Filter(entityClass = AdminMenu.class) Document filter,
                                 @ParameterObject Pageable pageable) {
        return adminMenuQueryRepository.findAll(filter, pageable);
    }
}
