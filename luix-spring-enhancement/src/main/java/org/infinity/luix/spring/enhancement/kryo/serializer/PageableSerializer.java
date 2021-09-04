package org.infinity.luix.spring.enhancement.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableSerializer extends Serializer<Pageable> {

    @Override
    public void write(Kryo kryo, Output output, Pageable pageable) {
        // Write pageNo
        output.writeInt(pageable.getPageNumber());
        // Write pageSize
        output.writeInt(pageable.getPageSize());
        // Write sort
        kryo.writeObjectOrNull(output, pageable.getSort(), Sort.class);
    }

    @Override
    public Pageable read(Kryo kryo, Input input, Class<? extends Pageable> type) {
        return kryo.readObject(input, PageRequest.class);
    }
}
