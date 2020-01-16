package org.infinity.app.common.domain;

import lombok.ToString;

import java.math.BigDecimal;

@ToString
public class Product {
    private Long       id;//id
    private String     sn;//产品编号
    private String     name;//产品名称
    private BigDecimal price;//产品价格

    public Product(Long id, String sn, String name, BigDecimal price) {
        this.id = id;
        this.sn = sn;
        this.name = name;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
