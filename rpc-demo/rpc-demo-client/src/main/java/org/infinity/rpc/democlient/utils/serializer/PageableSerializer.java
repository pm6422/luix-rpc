package org.infinity.rpc.democlient.utils.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.infinity.rpc.democlient.utils.serializer.PageRequestSerializer.readOrders;
import static org.infinity.rpc.democlient.utils.serializer.PageRequestSerializer.writePageable;

public class PageableSerializer extends Serializer<Pageable> {

    @Override
    public void write(Kryo kryo, Output output, Pageable request) {
        writePageable(output, request);
    }

    @Override
    public Pageable read(Kryo kryo, Input input, Class<Pageable> type) {
        // Read pageNo
        int pageNo = input.readInt();
        // Read pageSize
        int size = input.readInt();
        // Read sort
        int sortSize = input.readInt();
        List<Sort.Order> orders = readOrders(input, sortSize);
        return PageRequest.of(pageNo, size, sortSize == 0 ? Sort.unsorted() : Sort.by(orders));
    }
}
