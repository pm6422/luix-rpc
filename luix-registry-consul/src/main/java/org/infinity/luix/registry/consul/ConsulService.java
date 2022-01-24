package org.infinity.luix.registry.consul;

import lombok.Data;

import java.util.List;

@Data
public class ConsulService {

    private String       id;
    private String       name;
    private String       address;
    private Integer      port;
    private long         ttl;
    private List<String> tags;
}
