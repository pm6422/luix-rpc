package org.infinity.rpc.spring.enhancement.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.infinity.rpc.spring.enhancement.kryo.serializer.PageRequestSerializer.writePageable;

public class PageableSerializer extends Serializer<Pageable> {

    @Override
    public void write(Kryo kryo, Output output, Pageable pageable) {
        writePageable(kryo, output, pageable);
    }

    @Override
    public Pageable read(Kryo kryo, Input input, Class<? extends Pageable> type) {
        return kryo.readObject(input, PageRequest.class);
    }
}
