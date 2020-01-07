package org.infinity.app.server;

import org.infinity.app.common.IProductService;
import org.infinity.app.common.Product;
import org.infinity.rpc.server.RpcService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RpcService(IProductService.class)
public class ProductServiceImpl implements IProductService {
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
