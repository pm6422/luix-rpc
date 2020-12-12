package org.infinity.app.common.service;

import org.infinity.app.common.dto.AdminMenuTreeDTO;

import java.util.List;

public interface AdminMenuService {

    List<AdminMenuTreeDTO> getMenus();

}