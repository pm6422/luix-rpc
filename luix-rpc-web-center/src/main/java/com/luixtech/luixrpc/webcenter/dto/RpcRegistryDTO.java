package com.luixtech.luixrpc.webcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcRegistryDTO implements Serializable {
    private static final long serialVersionUID = 1617014195662314914L;

    private String type;

    private String identity;
}
