package org.infinity.luix.appclient.serializer;


import org.infinity.luix.democommon.domain.User;
import org.infinity.luix.spring.enhancement.kryo.serializer.*;
import org.infinity.luix.utilities.serializer.Serializer;
import org.infinity.luix.utilities.serializer.kryo.KryoUtils;
import org.infinity.luix.utilities.serviceloader.ServiceLoader;
import org.junit.Test;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.IOException;
import java.util.Optional;

import static org.infinity.luix.democommon.domain.Authority.ADMIN;
import static org.infinity.luix.democommon.domain.Authority.USER;
import static org.infinity.luix.utilities.serializer.Serializer.SERIALIZER_NAME_KRYO;

public class SerializerTests {

    private final Serializer kryoSerializer = ServiceLoader.forClass(Serializer.class).load(SERIALIZER_NAME_KRYO);

    static {
        KryoUtils.registerClass(Sort.class, new SortSerializer());
        KryoUtils.registerClass(PageRequest.class, new PageRequestSerializer());
        KryoUtils.registerClass(Pageable.class, new PageableSerializer());
        KryoUtils.registerClass(PageImpl.class, new PageImplSerializer());
        KryoUtils.registerClass(Page.class, new PageSerializer());
        KryoUtils.registerClass(Optional.class, new OptionalSerializer());
    }

    @Test
    public void testOptionalWithValue() throws IOException {
        User user = new User();
        user.setUserName("louis");
        Optional<User> optionalUser = Optional.of(user);
        byte[] serialized = kryoSerializer.serialize(optionalUser);
        Optional deserialized = kryoSerializer.deserialize(serialized, Optional.class);
        System.out.println(deserialized);
    }

    @Test
    public void testOptionalWithNullValue() throws IOException {
        Optional<User> optionalUser = Optional.ofNullable(null);
        byte[] serialized = kryoSerializer.serialize(optionalUser);
        Optional deserialized = kryoSerializer.deserialize(serialized, Optional.class);
        System.out.println(deserialized);
    }

    @Test
    public void test() {
        Criteria criteria = Criteria.where("name").in(ADMIN, USER).orOperator(Criteria.where("gender").ne("male"));
        Query query = Query.query(criteria);
        System.out.println();
    }
}
