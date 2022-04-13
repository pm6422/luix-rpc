package com.luixtech.rpc.webcenter.repository;

import com.luixtech.rpc.webcenter.domain.PersistentHttpTrace;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the PersistentHttpTrace entity.
 */
@Repository
public interface PersistenceHttpTraceRepository extends MongoRepository<PersistentHttpTrace, String> {

}
