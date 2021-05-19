package org.infinity.rpc.appclient.serializer;


import org.junit.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import static org.infinity.rpc.democommon.domain.Authority.ADMIN;
import static org.infinity.rpc.democommon.domain.Authority.USER;

public class SerializerTests {

    @Test
    public void test() {
        Criteria criteria = Criteria.where("name").in(ADMIN, USER).orOperator(Criteria.where("gender").ne("male"));
        Query query = Query.query(criteria);
        System.out.println();

    }
}
