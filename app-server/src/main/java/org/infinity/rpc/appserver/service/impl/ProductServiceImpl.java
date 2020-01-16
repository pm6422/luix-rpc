package org.infinity.rpc.appserver.service.impl;

import org.infinity.app.common.domain.Product;
import org.infinity.app.common.service.ProductService;
import org.infinity.rpc.server.annotation.Provider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Provider
public class ProductServiceImpl implements ProductService {
    @Override
    public void save(Product product) {
        System.out.println("产品保存成功: " + product);
    }

    @Override
    public void deleteById(Long productId) {
        System.out.println("产品删除成功: " + productId);
    }

    @Override
    public void update(Product product) {
        System.out.println("产品修改成功: " + product);
    }

    @Override
    public Product get(Long productId) {
        System.out.println("产品获取成功");
        return new Product(1L, UUID.randomUUID().toString(), "笔记本电脑", BigDecimal.TEN);
    }
}
