package com.luixtech.rpc.spring.enhancement.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

public class PageSerializer extends Serializer<Page<?>> {
    @Override
    public void write(Kryo kryo, Output output, Page<?> page) {
        // Write contents
        output.writeInt(CollectionUtils.isNotEmpty(page.getContent()) ? page.getContent().size() : 0);
        if (CollectionUtils.isNotEmpty(page.getContent())) {
            kryo.writeClass(output, page.getContent().get(0).getClass());
            for (Object item : page.getContent()) {
                kryo.writeObject(output, item);
            }
        }
        // Write Pageable
        kryo.writeObjectOrNull(output, page.getPageable(), Pageable.class);
        // Write total
        output.writeLong(page.getTotalElements());
    }

    @Override
    public Page<?> read(Kryo kryo, Input input, Class<? extends Page<?>> type) {
        return kryo.readObject(input, PageImpl.class);
    }
}