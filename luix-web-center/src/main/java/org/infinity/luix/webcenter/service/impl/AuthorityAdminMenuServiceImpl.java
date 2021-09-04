package org.infinity.luix.webcenter.service.impl;

import org.infinity.luix.webcenter.domain.AuthorityAdminMenu;
import org.infinity.luix.webcenter.repository.AuthorityAdminMenuRepository;
import org.infinity.luix.webcenter.service.AuthorityAdminMenuService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorityAdminMenuServiceImpl implements AuthorityAdminMenuService {

    @Resource
    private AuthorityAdminMenuRepository authorityAdminMenuRepository;

    @Override
    public Set<String> findAdminMenuIds(List<String> authorityNames) {
        return authorityAdminMenuRepository.findByAuthorityNameIn(authorityNames).stream()
                .map(AuthorityAdminMenu::getAdminMenuId).collect(Collectors.toSet());
    }
}