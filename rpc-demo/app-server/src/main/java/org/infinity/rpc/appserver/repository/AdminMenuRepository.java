package org.infinity.rpc.appserver.repository;

import org.infinity.app.common.domain.AdminMenu;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the AdminMenu entity.
 */
@Repository
public interface AdminMenuRepository extends MongoRepository<AdminMenu, String> {

}
