//package com.luixtech.rpc.appclient.controller;
//
//import lombok.extern.slf4j.Slf4j;
//import org.junit.Test;
//import org.springframework.util.StopWatch;
//import org.springframework.web.client.RestTemplate;
//
//@Slf4j
//public class AsyncControllerTests {
//    private RestTemplate restTemplate = new RestTemplate();
//
//    @Test
//    public void blockingResponse() {
//        StopWatch stopWatch = new StopWatch();
//        stopWatch.start();
//        restTemplate.getForObject("http://localhost:4001/api/tests/async/blocking-response", String.class);
//        stopWatch.stop();
//        log.info("BlockingResponse elapsed: {}ms", stopWatch.getTotalTimeMillis());
//    }
//
//    @Test
//    public void callableResponse() {
//        StopWatch stopWatch = new StopWatch();
//        stopWatch.start();
//        restTemplate.getForObject("http://localhost:4001/api/tests/async/callable-response", String.class);
//        stopWatch.stop();
//        log.info("CallableResponse elapsed: {}ms", stopWatch.getTotalTimeMillis());
//    }
//
//    @Test
//    public void deferredResult() {
//        StopWatch stopWatch = new StopWatch();
//        stopWatch.start();
//        restTemplate.getForObject("http://localhost:4001/api/tests/async/deferred-result", String.class);
//        stopWatch.stop();
//        log.info("DeferredResult elapsed: {}ms", stopWatch.getTotalTimeMillis());
//    }
//}
