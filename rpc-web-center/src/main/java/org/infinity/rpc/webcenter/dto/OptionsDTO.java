package org.infinity.rpc.webcenter.dto;

import lombok.Data;
import org.infinity.rpc.core.server.stub.OptionMeta;

import java.util.List;

@Data
public class OptionsDTO {

    private String url;

    private List<OptionMeta> options;
}
