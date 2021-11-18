package org.infinity.luix.webcenter.controller;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.webcenter.component.HttpHeaderCreator;
import org.infinity.luix.webcenter.domain.AdminMenu;
import org.infinity.luix.webcenter.repository.AdminMenuRepository;
import org.infinity.luix.webcenter.service.AdminMenuService;
import org.infinity.luix.webcenter.utils.HttpHeaderUtils;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.exception.DuplicationException;
import org.infinity.luix.webcenter.exception.DataNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for managing the admin menu.
 */
@RestController
@Slf4j
public class AdminMenuController {

    @Resource
    private AdminMenuRepository adminMenuRepository;
    @Resource
    private AdminMenuService    adminMenuService;
    @Resource
    private HttpHeaderCreator   httpHeaderCreator;

    @ApiOperation("create menu")
    @PostMapping("/api/admin-menus")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> create(
            @ApiParam(value = "menu", required = true) @Valid @RequestBody AdminMenu entity) {
        log.debug("REST request to create admin menu: {}", entity);
        adminMenuRepository.findOneByAppNameAndLevelAndSequence(entity.getAppName(), entity.getLevel(), entity.getSequence())
                .ifPresent((existingEntity) -> {
                    throw new DuplicationException(ImmutableMap.of("appName", entity.getAppName(), "level", entity.getLevel(), "sequence", entity.getSequence()));
                });
        adminMenuRepository.insert(entity);
        return ResponseEntity.status(HttpStatus.CREATED).headers(
                        httpHeaderCreator.createSuccessHeader("SM1001", entity.getCode()))
                .build();
    }

    @ApiOperation("find admin menu list")
    @GetMapping("/api/admin-menus")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<AdminMenu>> find(Pageable pageable,
                                                @ApiParam(value = "application name") @RequestParam(value = "appName", required = false) String appName) {
        Page<AdminMenu> adminMenus = adminMenuService.find(pageable, appName);
        HttpHeaders headers = HttpHeaderUtils.generatePageHeaders(adminMenus);
        return ResponseEntity.ok().headers(headers).body(adminMenus.getContent());
    }

    @ApiOperation("find admin menu by ID")
    @GetMapping("/api/admin-menus/{id}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<AdminMenu> findById(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        AdminMenu domain = adminMenuRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("update menu")
    @PutMapping("/api/admin-menus")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> update(
            @ApiParam(value = "new admin menu", required = true) @Valid @RequestBody AdminMenu domain) {
        log.debug("REST request to update admin menu: {}", domain);
        adminMenuRepository.findById(domain.getId()).orElseThrow(() -> new DataNotFoundException(domain.getId()));
        adminMenuRepository.save(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getCode())).build();
    }

    @ApiOperation("delete admin menu by ID")
    @DeleteMapping("/api/admin-menus/{id}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> delete(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete admin menu: {}", id);
        AdminMenu adminMenu = adminMenuRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        adminMenuRepository.deleteById(id);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", adminMenu.getCode())).build();
    }

    @ApiOperation("find parent menu list")
    @GetMapping("/api/admin-menus/parents")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<AdminMenu>> findParents(
            @ApiParam(value = "application name", required = true) @RequestParam(value = "appName") String appName,
            @ApiParam(value = "menu level", required = true) @RequestParam(value = "level") Integer level) {
        List<AdminMenu> all = adminMenuRepository.findByAppNameAndLevel(appName, level);
        return ResponseEntity.ok(all);
    }

    @ApiOperation("increase the order of management menus according to ID")
    @PutMapping("/api/admin-menus/move-up/{id}")
    @Secured({Authority.ADMIN})
    public void moveUp(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        adminMenuService.moveUp(id);
    }

    @ApiOperation("decrease the order of management menus according to ID")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "成功操作")})
    @PutMapping("/api/admin-menus/move-down/{id}")
    @Secured({Authority.ADMIN})
    public void moveDown(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        adminMenuService.moveDown(id);
    }

    @ApiOperation("copy menu")
    @GetMapping("/api/admin-menus/copy")
    @Secured({Authority.ADMIN})
    public void copyMenus(@ApiParam(value = "source application name", required = true, defaultValue = "Passport") @RequestParam(value = "sourceAppName") String sourceAppName,
                          @ApiParam(value = "destination application name", required = true) @RequestParam(value = "targetAppName") String targetAppName) {
        List<AdminMenu> sourceMenus = adminMenuRepository.findByAppName(sourceAppName);
        sourceMenus.forEach(menu -> {
            menu.setAppName(targetAppName);
            menu.setId(null);
        });
        adminMenuRepository.saveAll(sourceMenus);
    }

    @ApiOperation(value = "import menus", notes = "input file format: AppName, name, label, level, url, sequence for each row, separated by tab, and carriage return and line feed between rows")
    @PostMapping(value = "/api/admin-menus/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured({Authority.ADMIN})
    public void importData(@ApiParam(value = "file", required = true) @RequestPart MultipartFile file) throws IOException {
        List<String> lines = IOUtils.readLines(file.getInputStream(), StandardCharsets.UTF_8);
        List<AdminMenu> list = new ArrayList<>();
        for (String line : lines) {
            if (StringUtils.isNotEmpty(line)) {
                String[] lineParts = line.split("\t");
                AdminMenu entity = new AdminMenu(lineParts[0], lineParts[1], lineParts[2],
                        Integer.parseInt(lineParts[3]), lineParts[4], Integer.parseInt(lineParts[5]), null);
                list.add(entity);
            }
        }
        adminMenuRepository.insert(list);
    }
}
