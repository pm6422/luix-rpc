package org.infinity.rpc.demoserver.service.impl;

import org.infinity.rpc.core.server.annotation.RpcProvider;
import org.infinity.rpc.democommon.domain.AuthorityAdminMenu;
import org.infinity.rpc.democommon.service.AuthorityAdminMenuService;
import org.infinity.rpc.demoserver.repository.AuthorityAdminMenuRepository;

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