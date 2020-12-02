package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.AdminMenu;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data MongoDB repository for the AdminMenu entity.
 */
@Repository
public interface AdminMenuRepository extends MongoRepository<AdminMenu, String> {

    Optional<AdminMenu> findOneByAppNameAndLevelAndSequence(String appName, Integer level, Integer sequence);

    List<AdminMenu> findByAppNameAndIdIn(String appName, Set<String> ids);

    List<AdminMenu> findByAppNameAndIdInAndLevelGreaterThan(String appName, List<String> ids, Integer level);

    List<AdminMenu> findByAppName(String appName);

    List<AdminMenu> findByAppNameAndLevel(String appName, Integer level);

    List<AdminMenu> findByAppNameAndLevelOrderBySequenceAsc(String appName, Integer level);

}
