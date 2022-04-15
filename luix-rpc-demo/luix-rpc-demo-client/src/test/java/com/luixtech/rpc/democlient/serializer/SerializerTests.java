//package com.luixtech.rpc.appclient.serializer;
//
//
//import com.luixtech.rpc.spring.enhancement.kryo.serializer.*;
//import com.luixtech.rpc.serializer.Serializer;
//import com.luixtech.rpc.utilities.serializer.kryo.KryoUtils;
//import com.luixtech.rpc.utilities.serviceloader.ServiceLoader;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.springframework.data.domain.*;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//
//import static com.luixtech.rpc.democommon.domain.Authority.ADMIN;
//import static com.luixtech.rpc.democommon.domain.Authority.USER;
//import static com.luixtech.rpc.serializer.Serializer.SERIALIZER_NAME_KRYO;
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
