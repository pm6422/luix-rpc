package com.luixtech.rpc.demoserver.repository;

import com.luixtech.rpc.democommon.domain.AdminMenu;
import com.turkraft.springfilter.repository.DocumentExecutor;
import lombok.Getter;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.stereotype.Repository;

@Repository
@Getter
public class AdminMenuQueryRepository implements DocumentExecutor<AdminMenu, String> {

    private final MongoOperations                           mongoOperations;
    private final MongoEntityInformation<AdminMenu, String> metadata;

    public AdminMenuQueryRepository(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
        this.metadata = new MongoRepositoryFactory(mongoOperations).getEntityInformation(AdminMenu.class);
    }
}