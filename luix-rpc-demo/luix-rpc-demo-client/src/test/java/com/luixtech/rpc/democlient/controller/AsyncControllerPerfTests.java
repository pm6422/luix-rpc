//package com.luixtech.rpc.appclient.controller;
//
//import lombok.extern.slf4j.Slf4j;
//import org.databene.contiperf.PerfTest;
//import org.databene.contiperf.junit.ContiPerfRule;
//import org.junit.Rule;
//import org.junit.Test;
//import org.springframework.web.client.RestTemplate;
//
//@Slf4j
//public class AsyncControllerPerfTests {
//    @Rule
//    public  ContiPerfRule i            = new ContiPerfRule();
//    private RestTemplate  restTemplate = new RestTemplate();
//
//    @Test
//    @PerfTest(invocations = 10, threads = 16, rampUp = 100, warmUp = 10)
//    public void blockingResponse() {
//        restTemplate.getForObject("http://localhost:6010/api/tests/async/blocking-response", String.class);
//    }
//
//    @Test
//    @PerfTest(invocations = 10, threads = 16, rampUp = 100, warmUp = 10)
//    public void callableResponse() {
//        restTemplate.getForObject("http://localhost:6010/api/tests/async/callable-response", String.class);
//    }
//
//    @Test
//    @PerfTest(invocations = 10, threads = 16, rampUp = 100, warmUp = 10)
//    public void deferredResult() {
//        restTemplate.getForObject("http://localhost:6010/api/tests/async/deferred-result", String.class);
//    }
//}
