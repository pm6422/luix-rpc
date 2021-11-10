package org.infinity.luix.democommon.service;

import org.infinity.luix.democommon.dto.AdminMenuTreeDTO;

import java.util.List;

public interface AdminMenuService {

    List<AdminMenuTreeDTO> getMenus();

}