package com.luixtech.rpc.serializer;

import com.luixtech.rpc.serializer.entity.AdminMenu;
import com.luixtech.utilities.serviceloader.ServiceLoader;
import lombok.extern.slf4j.Slf4j;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

@Slf4j
public class SerializerPerfTests {
    @Rule
    public ContiPerfRule i = new ContiPerfRule();

    private Serializer kryoSerializer     = ServiceLoader.forClass(Serializer.class).load(Serializer.SERIALIZER_NAME_KRYO);
    private Serializer hessian2Serializer = ServiceLoader.forClass(Serializer.class).load(Serializer.SERIALIZER_NAME_HESSIAN2);

    private final AdminMenu menu = new AdminMenu("M12383213", "应用", 1,
            "https://www.baidu.com", 1, "7233434321223");

//    @Test
//    @PerfTest(invocations = 10000, threads = 16, rampUp = 100, warmUp = 10)
//    public void kryoPerf() throws IOException {
//        byte[] serialized = kryoSerializer.serialize(menu);
//        kryoSerializer.deserialize(serialized, AdminMenu.class);
//    }

    @Test
    @PerfTest(invocations = 10000, threads = 16, rampUp = 100, warmUp = 10)
    public void hessian2Perf() throws IOException {
        byte[] serialized = hessian2Serializer.serialize(menu);
        hessian2Serializer.deserialize(serialized, AdminMenu.class);
    }
}
