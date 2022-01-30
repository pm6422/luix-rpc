package org.infinity.luix.registry.consul;

import lombok.Data;

@Data
public class ConsulResponse<T> {
    private T       value;
    private Long    consulIndex;
    private Boolean consulKnownLeader;
    private Long    consulLastContact;
}
