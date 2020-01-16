package org.infinity.rpc.appserver.service.impl;

import org.infinity.rpc.appserver.domain.AdminMenu;
import org.infinity.rpc.appserver.entity.MenuTree;
import org.infinity.rpc.appserver.entity.MenuTreeNode;
import org.infinity.rpc.appserver.repository.AdminMenuRepository;
import org.infinity.rpc.appserver.service.AdminMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminMenuServiceImpl implements AdminMenuService {

    @Autowired
    private AdminMenuRepository       adminMenuRepository;

    @Override
    public List<MenuTreeNode> getMenus() {
        List<AdminMenu> adminMenus = adminMenuRepository.findAll();
        return this.groupAdminMenu(adminMenus);
    }

    private List<MenuTreeNode> groupAdminMenu(List<AdminMenu> menus) {
        MenuTree tree = new MenuTree(menus.stream().map(menu -> menu.asNode()).collect(Collectors.toList()));
        return tree.getChildren();
    }
}