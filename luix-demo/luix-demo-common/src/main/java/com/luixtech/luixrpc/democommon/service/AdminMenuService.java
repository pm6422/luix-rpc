package com.luixtech.luixrpc.democommon.service;

import com.luixtech.luixrpc.democommon.dto.AdminMenuTreeDTO;

import java.util.List;

public interface AdminMenuService {

    List<AdminMenuTreeDTO> getMenus();

}