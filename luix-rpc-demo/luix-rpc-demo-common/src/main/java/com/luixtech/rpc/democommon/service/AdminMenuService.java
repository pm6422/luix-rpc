package com.luixtech.rpc.democommon.service;

import com.luixtech.rpc.democommon.dto.AdminMenuTreeDTO;

import java.util.List;

public interface AdminMenuService {

    List<AdminMenuTreeDTO> getMenus();

}