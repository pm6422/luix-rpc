package org.infinity.rpc.appserver.controller;

import io.swagger.annotations.*;
import org.infinity.app.common.domain.App;
import org.infinity.app.common.dto.AppDTO;
import org.infinity.app.common.service.AppService;
import org.infinity.rpc.appserver.exception.NoDataException;
import org.infinity.rpc.appserver.repository.AppRepository;
import org.infinity.rpc.appserver.utils.HttpHeaderCreator;
import org.infinity.rpc.appserver.utils.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * REST controller for managing apps.
 */
@RestController
@Api(tags = "应用管理")
public class AppController {

    private static final Logger            LOGGER = LoggerFactory.getLogger(AppController.class);
    @Autowired
    private              AppRepository     appRepository;
    @Autowired
    private              AppService        appService;
    @Autowired
    private              HttpHeaderCreator httpHeaderCreator;

    @ApiOperation("创建应用")
    @ApiResponses(value = {@ApiResponse(code = SC_CREATED, message = "成功创建")})
    @PostMapping("/api/app/apps")
    public ResponseEntity<Void> create(@ApiParam(value = "应用信息", required = true) @Valid @RequestBody AppDTO dto) {
        LOGGER.debug("REST request to create app: {}", dto);
        appService.insert(dto.getName(), dto.getEnabled(), dto.getAuthorities());
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("notification.app.created", dto.getName())).build();
    }

    @ApiOperation("获取应用列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("/api/app/apps")
    public ResponseEntity<List<AppDTO>> find(Pageable pageable) throws URISyntaxException {
        Page<App> apps = appRepository.findAll(pageable);
        List<AppDTO> DTOs = apps.getContent().stream().map(entity -> entity.asDTO()).collect(Collectors.toList());
        HttpHeaders headers = PaginationUtils.generatePaginationHttpHeaders(apps, "/api/app/apps");
        return ResponseEntity.ok().headers(headers).body(DTOs);
    }

    @ApiOperation("获取所有应用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("/api/app/apps/all")
    public ResponseEntity<List<AppDTO>> findAll() {
        List<AppDTO> appDTOs = appRepository.findAll().stream().map(app -> app.asDTO()).collect(Collectors.toList());
        return ResponseEntity.ok(appDTOs);
    }

    @ApiOperation("根据应用名称检索应用信息")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "应用信息不存在")})
    @GetMapping("/api/app/apps/{name}")
    public ResponseEntity<AppDTO> findById(@ApiParam(value = "应用名称", required = true) @PathVariable String name) {
        App app = appRepository.findById(name).get();
        return ResponseEntity.ok(new AppDTO(name, app.getEnabled(), null));
    }

    @ApiOperation("更新应用信息")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功更新"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "应用信息不存在")})
    @PutMapping("/api/app/apps")
    public ResponseEntity<Void> update(@ApiParam(value = "新的应用信息", required = true) @Valid @RequestBody AppDTO dto) {
        LOGGER.debug("REST request to update app: {}", dto);
        appRepository.findById(dto.getName()).orElseThrow(() -> new NoDataException(dto.getName()));
        appService.update(dto.getName(), dto.getEnabled(), dto.getAuthorities());
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.app.updated", dto.getName())).build();
    }

    @ApiOperation(value = "根据应用名称删除应用信息", notes = "数据有可能被其他数据所引用，删除之后可能出现一些问题")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功删除"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "应用信息不存在")})
    @DeleteMapping("/api/app/apps/{name}")
    public ResponseEntity<Void> delete(@ApiParam(value = "应用名称", required = true) @PathVariable String name) {
        LOGGER.debug("REST request to delete app: {}", name);
        appRepository.findById(name).orElseThrow(() -> new NoDataException(name));
        appRepository.deleteById(name);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.app.deleted", name)).build();
    }
}
