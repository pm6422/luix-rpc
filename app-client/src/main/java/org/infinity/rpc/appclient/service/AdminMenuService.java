package org.infinity.rpc.appclient.service;

import org.infinity.rpc.appclient.entity.MenuTreeNode;

import java.util.List;

public interface AdminMenuService {

    List<MenuTreeNode> getMenus();
}