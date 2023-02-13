package com.luixtech.rpc.democommon.service;

import com.luixtech.rpc.democommon.dto.AdminMenuDTO;

import java.util.List;

public interface AdminMenuService {

    List<AdminMenuDTO> getMenus();

}