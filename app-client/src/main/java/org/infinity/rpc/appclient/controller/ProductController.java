package org.infinity.rpc.appclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.Product;
import org.infinity.app.common.ProductService;
import org.infinity.app.common.UserService;
import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ProductController {

    @Consumer
    private ProductService productService;
    @Consumer
    private UserService    userService;

    @GetMapping("/api/product/product")
    public Product getProduct() {
        int count = userService.count();
        log.debug("User count: {}", count);
        Product product = productService.get(1L);
        return product;
    }
}
