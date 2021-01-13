package org.infinity.rpc.democommon.service;

import org.infinity.rpc.democommon.dto.AdminMenuTreeDTO;

import java.util.List;

public interface AdminMenuService {

    List<AdminMenuTreeDTO> getMenus();

}