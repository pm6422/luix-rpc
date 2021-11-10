package org.infinity.luix.demoserver.service.impl;

import org.infinity.luix.core.server.annotation.RpcProvider;
import org.infinity.luix.democommon.domain.AuthorityAdminMenu;
import org.infinity.luix.democommon.service.AuthorityAdminMenuService;
import org.infinity.luix.demoserver.repository.AuthorityAdminMenuRepository;

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