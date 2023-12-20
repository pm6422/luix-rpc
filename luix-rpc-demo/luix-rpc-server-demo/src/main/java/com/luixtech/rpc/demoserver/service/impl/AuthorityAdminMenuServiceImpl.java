package com.luixtech.rpc.demoserver.service.impl;

import com.luixtech.rpc.core.server.annotation.RpcProvider;
import com.luixtech.rpc.democommon.domain.AuthorityAdminMenu;
import com.luixtech.rpc.democommon.service.AuthorityAdminMenuService;
import com.luixtech.rpc.demoserver.repository.AuthorityAdminMenuRepository;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RpcProvider
@AllArgsConstructor
public class AuthorityAdminMenuServiceImpl implements AuthorityAdminMenuService {
    private final AuthorityAdminMenuRepository authorityAdminMenuRepository;

    @Override
    public Set<String> findAdminMenuIds(List<String> authorityNames) {
        return authorityAdminMenuRepository.findByAuthorityNameIn(authorityNames).stream()
                .map(AuthorityAdminMenu::getAdminMenuId).collect(Collectors.toSet());
    }
}