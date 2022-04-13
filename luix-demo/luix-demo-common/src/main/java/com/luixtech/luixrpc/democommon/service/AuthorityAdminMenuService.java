package com.luixtech.luixrpc.democommon.service;

import java.util.List;
import java.util.Set;

public interface AuthorityAdminMenuService {

    Set<String> findAdminMenuIds(List<String> authorityNames);

}