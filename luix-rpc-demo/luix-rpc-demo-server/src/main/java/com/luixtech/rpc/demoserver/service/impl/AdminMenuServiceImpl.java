package com.luixtech.rpc.demoserver.service.impl;

import com.luixtech.rpc.core.server.annotation.RpcProvider;
import com.luixtech.rpc.democommon.domain.AdminMenu;
import com.luixtech.rpc.democommon.dto.AdminMenuDTO;
import com.luixtech.rpc.democommon.service.AdminMenuService;
import com.luixtech.rpc.demoserver.repository.AdminMenuRepository;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RpcProvider
public class AdminMenuServiceImpl implements AdminMenuService {
    @Resource
    private AdminMenuRepository adminMenuRepository;

    @Override
    public List<AdminMenuDTO> getMenus() {
        List<AdminMenu> adminMenus = adminMenuRepository.findAll();
        return convertToTree(adminMenus);
    }

    private List<AdminMenuDTO> convertToTree(List<AdminMenu> menus) {
        return convertToTree(menus, "0");
    }

    private List<AdminMenuDTO> convertToTree(List<AdminMenu> menus, String parentId) {
        return menus.stream()
                // 过滤父节点
                .filter(parent -> parentId.equals(parent.getParentId()))
                .map(m -> m.toDTO())
                // 排序
                .sorted(Comparator.comparing(AdminMenuDTO::getSequence))
                // 把父节点children递归赋值成为子节点
                .peek(node -> node.setChildren(convertToTree(menus, node.getId())))
                .collect(Collectors.toList());
    }
}