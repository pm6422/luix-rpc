package com.luixtech.rpc.demoserver.repository;

import com.luixtech.rpc.democommon.domain.DictItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DictItem entity.
 */
@Repository
public interface DictItemRepository extends MongoRepository<DictItem, String> {

    List<DictItem> findByDictCodeAndDictItemCode(String dictCode, String dictItemCode);
}
