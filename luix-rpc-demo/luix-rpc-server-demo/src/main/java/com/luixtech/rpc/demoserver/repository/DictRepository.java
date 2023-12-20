package com.luixtech.rpc.demoserver.repository;

import com.luixtech.rpc.democommon.domain.Dict;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the Dict entity.
 */
@Repository
public interface DictRepository extends MongoRepository<Dict, String> {

    Optional<Dict> findOneByDictCode(String dictCode);

}
