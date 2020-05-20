package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.AdminMenu;
import org.infinity.rpc.webcenter.entity.MenuTreeNode;

import java.util.List;

public interface AdminMenuService {

    List<MenuTreeNode> getAllAuthorityMenus(String appName, String enabledAuthority);

    List<MenuTreeNode> getAuthorityMenus(String appName, List<String> enabledAuthorities);

    List<AdminMenu> getAuthorityLinks(String appName, List<String> enabledAuthorities);

    void raiseSeq(String id);

    void lowerSeq(String id);
}