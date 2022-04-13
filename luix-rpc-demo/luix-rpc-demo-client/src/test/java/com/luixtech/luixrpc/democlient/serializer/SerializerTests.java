//package com.luixtech.luixrpc.appclient.serializer;
//
//
//import com.luixtech.luixrpc.spring.enhancement.kryo.serializer.*;
//import com.luixtech.luixrpc.utilities.serializer.Serializer;
//import com.luixtech.luixrpc.utilities.serializer.kryo.KryoUtils;
//import com.luixtech.luixrpc.utilities.serviceloader.ServiceLoader;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.springframework.data.domain.*;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//
//import static com.luixtech.luixrpc.democommon.domain.Authority.ADMIN;
//import static com.luixtech.luixrpc.democommon.domain.Authority.USER;
//import static com.luixtech.luixrpc.utilities.serializer.Serializer.SERIALIZER_NAME_KRYO;
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
