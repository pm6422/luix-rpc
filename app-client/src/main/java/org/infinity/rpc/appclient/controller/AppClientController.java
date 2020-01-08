package org.infinity.rpc.appclient.controller;

import org.infinity.app.common.IProductService;
import org.infinity.app.common.Product;
import org.infinity.rpc.client.RpcClientProxy;
import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppClientController implements InitializingBean {

    @Autowired
    private RpcClientProxy  rpcClientProxy;
    @Consumer
    private IProductService productService;

    @Override
    public void afterPropertiesSet() throws Exception {
        productService = rpcClientProxy.getProxy(IProductService.class);
    }

    @GetMapping("/api/app-client/product")
    public Product getProduct() {
        Product product = productService.get(1L);
        return product;
    }
}
