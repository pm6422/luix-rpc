package org.infinity.rpc.appserver.service.impl;

import org.infinity.app.common.domain.AdminMenu;
import org.infinity.app.common.entity.MenuTree;
import org.infinity.app.common.entity.MenuTreeNode;
import org.infinity.app.common.service.AdminMenuService;
import org.infinity.rpc.appserver.repository.AdminMenuRepository;
import org.infinity.rpc.core.server.annotation.Provider;

import java.util.List;
import java.util.stream.Collectors;

@Provider
public class AdminMenuServiceImpl implements AdminMenuService {

    private final AdminMenuRepository adminMenuRepository;

    public AdminMenuServiceImpl(AdminMenuRepository adminMenuRepository) {
        this.adminMenuRepository = adminMenuRepository;
    }

    @Override
    public List<MenuTreeNode> getMenus() {
        List<AdminMenu> adminMenus = adminMenuRepository.findAll();
        return this.groupAdminMenu(adminMenus);
    }

    private List<MenuTreeNode> groupAdminMenu(List<AdminMenu> menus) {
        MenuTree tree = new MenuTree(menus.stream().map(AdminMenu::asNode).collect(Collectors.toList()));
        return tree.getChildren();
    }
}