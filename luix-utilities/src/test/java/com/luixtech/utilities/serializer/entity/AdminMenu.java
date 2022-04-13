package com.luixtech.utilities.serializer.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the AdminMenu entity.
 */
@Data
@NoArgsConstructor
public class AdminMenu implements Serializable {

    private static final long   serialVersionUID = 5423774898556939254L;
    public static final  String FIELD_LEVEL      = "level";
    public static final  String FIELD_SEQUENCE   = "sequence";

    private String code;

    private String name;

    private Integer level;

    private String url;

    private Integer sequence;

    private String parentId;

    private Boolean checked;

    public AdminMenu(String code, String name, Integer level, String url,
                     Integer sequence, String parentId) {
        super();
        this.code = code;
        this.name = name;
        this.level = level;
        this.url = url;
        this.sequence = sequence;
        this.parentId = parentId;
    }
}