//package org.infinity.luix.appclient.serializer;
//
//
//import org.infinity.luix.spring.enhancement.kryo.serializer.*;
//import org.infinity.luix.utilities.serializer.Serializer;
//import org.infinity.luix.utilities.serializer.kryo.KryoUtils;
//import org.infinity.luix.utilities.serviceloader.ServiceLoader;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.springframework.data.domain.*;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//
//import static org.infinity.luix.democommon.domain.Authority.ADMIN;
//import static org.infinity.luix.democommon.domain.Authority.USER;
//import static org.infinity.luix.utilities.serializer.Serializer.SERIALIZER_NAME_KRYO;
//
//public class SerializerTests {
//
//    private static Serializer kryoSerializer;
//
//    @BeforeClass
//    public static void setUp() {
//        KryoUtils.registerClass(Sort.class, new SortSerializer());
//        KryoUtils.registerClass(PageRequest.class, new PageRequestSerializer());
//        KryoUtils.registerClass(Pageable.class, new PageableSerializer());
//        KryoUtils.registerClass(PageImpl.class, new PageImplSerializer());
//        KryoUtils.registerClass(Page.class, new PageSerializer());
//
//        kryoSerializer = ServiceLoader.forClass(Serializer.class).load(SERIALIZER_NAME_KRYO);
//    }
//
//    @Test
//    public void test() {
//        Criteria criteria = Criteria.where("name").in(ADMIN, USER).orOperator(Criteria.where("gender").ne("male"));
//        Query query = Query.query(criteria);
//        System.out.println();
//    }
//}
