package org.infinity.rpc.appclient.service.impl;

import com.codahale.metrics.annotation.Timed;
import org.infinity.rpc.appclient.domain.AdminMenu;
import org.infinity.rpc.appclient.entity.MenuTree;
import org.infinity.rpc.appclient.entity.MenuTreeNode;
import org.infinity.rpc.appclient.repository.AdminMenuRepository;
import org.infinity.rpc.appclient.service.AdminMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminMenuServiceImpl implements AdminMenuService {

    @Autowired
    private AdminMenuRepository       adminMenuRepository;

    @Override
    @Timed
    public List<MenuTreeNode> getMenus() {
        List<AdminMenu> adminMenus = adminMenuRepository.findAll();
        return this.groupAdminMenu(adminMenus);
    }

    private List<MenuTreeNode> groupAdminMenu(List<AdminMenu> menus) {
        MenuTree tree = new MenuTree(menus.stream().map(menu -> menu.asNode()).collect(Collectors.toList()));
        return tree.getChildren();
    }
}