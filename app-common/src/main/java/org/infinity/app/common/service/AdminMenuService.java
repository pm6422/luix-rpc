package org.infinity.app.common.service;

import org.infinity.app.common.entity.MenuTreeNode;

import java.util.List;

public interface AdminMenuService {

    List<MenuTreeNode> getMenus();
}