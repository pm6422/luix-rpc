package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.*;
import org.infinity.rpc.webcenter.domain.App;
import org.infinity.rpc.webcenter.domain.AppAuthority;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.dto.AppDTO;
import org.infinity.rpc.webcenter.exception.NoDataException;
import org.infinity.rpc.webcenter.repository.AppAuthorityRepository;
import org.infinity.rpc.webcenter.repository.AppRepository;
import org.infinity.rpc.webcenter.service.AppService;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.infinity.rpc.webcenter.utils.HttpHeaderCreator;
import org.infinity.rpc.webcenter.utils.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * REST controller for managing apps.
 */
@RestController
@Api(tags = "应用管理")
public class AppController {
    @Autowired
    private              RegistryService   registryService;
    @Autowired
    private              HttpHeaderCreator httpHeaderCreator;

//    @ApiOperation("获取应用列表")
//    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
//    @GetMapping("/api/app/apps")
//    @Secured({Authority.ADMIN})
//    public ResponseEntity<List<AppDTO>> find(Pageable pageable) throws URISyntaxException {
//        Page<App> apps = appRepository.findAll(pageable);
//        List<AppDTO> DTOs = apps.getContent().stream().map(entity -> entity.asDTO()).collect(Collectors.toList());
//        HttpHeaders headers = PaginationUtils.generatePaginationHttpHeaders(apps, "/api/app/apps");
//        return ResponseEntity.ok().headers(headers).body(DTOs);
//    }

    @ApiOperation("获取所有应用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("/api/app/apps/all")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<String>> findAll() {
        List<String> applications = registryService.getAllApplications();
        return ResponseEntity.ok(applications);
    }

//    @ApiOperation("根据应用名称检索应用信息")
//    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取"),
//            @ApiResponse(code = SC_BAD_REQUEST, message = "应用信息不存在")})
//    @GetMapping("/api/app/apps/{name}")
//    @Secured({Authority.ADMIN})
//    public ResponseEntity<AppDTO> findById(@ApiParam(value = "应用名称", required = true) @PathVariable String name) {
//        App app = appRepository.findById(name).get();
//        List<AppAuthority> appAuthorities = appAuthorityRepository.findByAppName(name);
//        Set<String> authorities = appAuthorities.stream().map(item -> item.getAuthorityName())
//                .collect(Collectors.toSet());
//        return ResponseEntity.ok(new AppDTO(name, app.getEnabled(), authorities));
//    }
}
