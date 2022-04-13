//package com.luixtech.rpc.appclient.serializer;
//
//import lombok.extern.slf4j.Slf4j;
//import org.databene.contiperf.PerfTest;
//import org.databene.contiperf.junit.ContiPerfRule;
//import com.luixtech.rpc.appclient.serializer.service.AppServiceImpl;
//import com.luixtech.rpc.democommon.domain.App;
//import com.luixtech.rpc.spring.enhancement.kryo.serializer.*;
//import com.luixtech.rpc.utilities.serializer.Serializer;
//import com.luixtech.rpc.utilities.serializer.kryo.KryoUtils;
//import com.luixtech.rpc.utilities.serviceloader.ServiceLoader;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.springframework.data.domain.*;
//
//import java.io.IOException;
//
//import static com.luixtech.rpc.utilities.serializer.Serializer.SERIALIZER_NAME_HESSIAN2;
//import static com.luixtech.rpc.utilities.serializer.Serializer.SERIALIZER_NAME_KRYO;
//
///**
// * Refer to {@link com.luixtech.rpc.utilities.serializer.SerializerPerfTests} for another test cases
// */
//@Slf4j
//public class SerializerPerfTests {
//
//    @Rule
//    public         ContiPerfRule i = new ContiPerfRule();
//    private static Serializer    kryoSerializer;
//    private static Serializer    hessian2Serializer;
//
//    @BeforeAll
//    public static void setUp() {
//        KryoUtils.registerClass(Sort.class, new SortSerializer());
//        KryoUtils.registerClass(PageRequest.class, new PageRequestSerializer());
//        KryoUtils.registerClass(Pageable.class, new PageableSerializer());
//        KryoUtils.registerClass(PageImpl.class, new PageImplSerializer());
//        KryoUtils.registerClass(Page.class, new PageSerializer());
//
//        kryoSerializer = ServiceLoader.forClass(Serializer.class).load(SERIALIZER_NAME_KRYO);
//        hessian2Serializer = ServiceLoader.forClass(Serializer.class).load(SERIALIZER_NAME_HESSIAN2);
//    }
//
//    @Test
//    @PerfTest(invocations = 10000, threads = 16, rampUp = 100, warmUp = 10)
//    public void kryoPerf() throws IOException {
//        AppServiceImpl appServiceImpl = new AppServiceImpl();
//        Pageable pageable = PageRequest.of(0, 100, Sort.by("name").descending());
//        Page<App> all = appServiceImpl.findAll(pageable);
//        byte[] serialized = kryoSerializer.serialize(all);
//        kryoSerializer.deserialize(serialized, Page.class);
//    }
//
//    @Test
//    @PerfTest(invocations = 10000, threads = 16, rampUp = 100, warmUp = 10)
//    public void hessian2Perf() throws IOException {
//        AppServiceImpl appServiceImpl = new AppServiceImpl();
//        Pageable pageable = PageRequest.of(0, 100, Sort.by("name").descending());
//        Page<App> all = appServiceImpl.findAll(pageable);
//        byte[] serialized = hessian2Serializer.serialize(all);
//        hessian2Serializer.deserialize(serialized, Page.class);
//    }
//}
