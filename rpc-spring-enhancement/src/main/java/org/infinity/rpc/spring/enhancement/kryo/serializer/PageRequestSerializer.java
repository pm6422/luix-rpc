package org.infinity.rpc.spring.enhancement.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageRequestSerializer extends Serializer<PageRequest> {
    @Override
    public void write(Kryo kryo, Output output, PageRequest request) {
        writePageable(kryo, output, request);
    }

    @Override
    public PageRequest read(Kryo kryo, Input input, Class<? extends PageRequest> type) {
        // Read pageNo
        int page = input.readInt();
        // Read pageSize
        int size = input.readInt();
        // Read sort
        Sort sort = kryo.readObjectOrNull(input, Sort.class);
        return PageRequest.of(page, size, sort);
    }

    public static void writePageable(Kryo kryo, Output output, Pageable pageable) {
        // Write pageNo
        output.writeInt(pageable.getPageNumber());
        // Write pageSize
        output.writeInt(pageable.getPageSize());
        // Write sort
        kryo.writeObjectOrNull(output, pageable.getSort(), Sort.class);
    }
}