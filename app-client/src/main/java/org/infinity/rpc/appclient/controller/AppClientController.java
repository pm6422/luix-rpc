package org.infinity.rpc.appclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.IProductService;
import org.infinity.app.common.Product;
import org.infinity.rpc.client.RpcClientProxy;
import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class AppClientController implements InitializingBean {

    @Autowired
    private RpcClientProxy rpcClientProxy;

    @Consumer
//    @Autowired
    private IProductService productService;

    public AppClientController() {
    }

    public AppClientController(RpcClientProxy rpcClientProxy) {
        this.rpcClientProxy = rpcClientProxy;
    }


    public AppClientController(RpcClientProxy rpcClientProxy, IProductService productService) {
        this.rpcClientProxy = rpcClientProxy;
        this.productService = productService;
    }

    public AppClientController(IProductService productService) {
        this.productService = productService;
    }

    //    @Autowired
//    public void setRpcClientProxy(RpcClientProxy rpcClientProxy) {
//        this.rpcClientProxy = rpcClientProxy;
//    }
//
//    @Autowired
//    public void setProductService(IProductService productService) {
//        this.productService = productService;
//    }

    @Override
    public void afterPropertiesSet() throws Exception {
        productService = rpcClientProxy.getProxy(IProductService.class);
        log.debug("get");
    }

    @GetMapping("/api/app-client/product")
    public Product getProduct() {
        Product product = productService.get(1L);
        return product;
    }
}
