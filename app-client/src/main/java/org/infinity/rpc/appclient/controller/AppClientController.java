package org.infinity.rpc.appclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.IProductService;
import org.infinity.app.common.Product;
import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class AppClientController {

    @Consumer
    private IProductService productService;

    @Autowired
    public void setProductService(IProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/app-client/product")
    public Product getProduct() {
        Product product = productService.get(1L);
        return product;
    }
}
