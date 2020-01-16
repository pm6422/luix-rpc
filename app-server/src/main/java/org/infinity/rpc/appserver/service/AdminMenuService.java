package org.infinity.rpc.appserver.service;

import org.infinity.rpc.appserver.entity.MenuTreeNode;

import java.util.List;

public interface AdminMenuService {

    List<MenuTreeNode> getMenus();
}