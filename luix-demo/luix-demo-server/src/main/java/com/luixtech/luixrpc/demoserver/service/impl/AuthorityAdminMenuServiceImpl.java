package com.luixtech.luixrpc.demoserver.service.impl;

import com.luixtech.luixrpc.core.server.annotation.RpcProvider;
import com.luixtech.luixrpc.democommon.domain.AuthorityAdminMenu;
import com.luixtech.luixrpc.democommon.service.AuthorityAdminMenuService;
import com.luixtech.luixrpc.demoserver.repository.AuthorityAdminMenuRepository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RpcProvider
public class AuthorityAdminMenuServiceImpl implements AuthorityAdminMenuService {

    @Resource
    private AuthorityAdminMenuRepository authorityAdminMenuRepository;

    @Override
    public Set<String> findAdminMenuIds(List<String> authorityNames) {
        return authorityAdminMenuRepository.findByAuthorityNameIn(authorityNames).stream()
                .map(AuthorityAdminMenu::getAdminMenuId).collect(Collectors.toSet());
    }
}