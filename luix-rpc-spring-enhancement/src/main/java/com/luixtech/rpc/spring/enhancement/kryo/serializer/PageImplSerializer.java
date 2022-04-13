package com.luixtech.rpc.spring.enhancement.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

public class PageImplSerializer extends Serializer<PageImpl<?>> {
    @Override
    public void write(Kryo kryo, Output output, PageImpl<?> page) {
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public PageImpl<?> read(Kryo kryo, Input input, Class<? extends PageImpl<?>> type) {
        // Read contents
        int contentSize = input.readInt();
        List contents = new ArrayList<>(contentSize);
        if (contentSize != 0) {
            Registration registration = kryo.readClass(input);
            for (int i = 0; i < contentSize; i++) {
                contents.add(kryo.readObject(input, registration.getType()));
            }
        }

        // Read Pageable
        Pageable pageable = kryo.readObjectOrNull(input, Pageable.class);
        // Read total
        return new PageImpl(contents, pageable, input.readLong());
    }
}