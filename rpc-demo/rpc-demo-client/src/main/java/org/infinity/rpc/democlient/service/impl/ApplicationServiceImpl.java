package org.infinity.rpc.democlient.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.democlient.domain.Application;
import org.infinity.rpc.democlient.service.ApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import static org.infinity.rpc.democlient.domain.Application.*;
import static org.infinity.rpc.democlient.domain.Provider.FIELD_REGISTRY_IDENTITY;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    private final MongoTemplate mongoTemplate;

    public ApplicationServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Application> find(Pageable pageable, String registryUrl, String name, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryUrl));
        if (StringUtils.isNotEmpty(name)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_NAME).regex(pattern));
        }
        if (active != null) {
            if (Boolean.TRUE.equals(active)) {
                // or criteria
                Criteria criteria = new Criteria().orOperator(
                        Criteria.where(FIELD_ACTIVE_PROVIDER).is(true),
                        Criteria.where(FIELD_ACTIVE_CONSUMER).is(true));
                query.addCriteria(criteria);
            } else {
                query.addCriteria(Criteria.where(FIELD_ACTIVE_PROVIDER).is(false));
                query.addCriteria(Criteria.where(FIELD_ACTIVE_CONSUMER).is(false));
            }
        }

        long totalCount = mongoTemplate.count(query, Application.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, Application.class), pageable, totalCount);
    }
}
