package org.infinity.rpc.webcenter.service.impl;

import org.infinity.rpc.webcenter.domain.AuthorityAdminMenu;
import org.infinity.rpc.webcenter.repository.AuthorityAdminMenuRepository;
import org.infinity.rpc.webcenter.service.AuthorityAdminMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorityAdminMenuServiceImpl implements AuthorityAdminMenuService {

    @Autowired
    private AuthorityAdminMenuRepository authorityAdminMenuRepository;

    @Override
    public Set<String> findAdminMenuIdSetByAuthorityNameIn(List<String> authorityNames) {
        return authorityAdminMenuRepository.findByAuthorityNameIn(authorityNames).stream()
                .map(AuthorityAdminMenu::getAdminMenuId).collect(Collectors.toSet());
    }

}