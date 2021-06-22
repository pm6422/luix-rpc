package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.Consumer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the Consumer entity.
 */
@Repository
public interface ConsumerRepository extends MongoRepository<Consumer, String> {

    List<Consumer> findByInterfaceName(String interfaceName);
}
