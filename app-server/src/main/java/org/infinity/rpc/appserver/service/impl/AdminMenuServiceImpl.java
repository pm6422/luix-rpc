package org.infinity.rpc.appserver.service.impl;

import org.infinity.app.common.domain.AdminMenu;
import org.infinity.app.common.entity.MenuTree;
import org.infinity.app.common.entity.MenuTreeNode;
import org.infinity.app.common.service.AdminMenuService;
import org.infinity.rpc.appserver.repository.AdminMenuRepository;
import org.infinity.rpc.core.server.annotation.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Provider
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