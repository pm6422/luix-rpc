package org.infinity.rpc.appclient.repository;

import org.infinity.rpc.appclient.domain.AuthorityAdminMenu;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the AuthorityAdminMenu entity.
 */
@Repository
public interface AuthorityAdminMenuRepository extends MongoRepository<AuthorityAdminMenu, String> {

    List<AuthorityAdminMenu> findByAuthorityNameIn(List<String> authorityNames);

    List<AuthorityAdminMenu> findByAuthorityName(String authorityName);

    void deleteByAuthorityNameAndAdminMenuIdIn(String authorityName, List<String> adminMenuIds);
}
