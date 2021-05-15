package org.infinity.rpc.demoserver.utils.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PageRequestSerializer extends Serializer<PageRequest> {
    @Override
    public void write(Kryo kryo, Output output, PageRequest request) {
        writePageable(output, request);
    }

    @Override
    public PageRequest read(Kryo kryo, Input input, Class<PageRequest> type) {
        // Read pageNo
        int page = input.readInt();
        // Read pageSize
        int size = input.readInt();
        // Read sort
        int sortSize = input.readInt();
        List<Sort.Order> orders = readOrders(input, sortSize);
        return PageRequest.of(page, size, sortSize == 0 ? Sort.unsorted() : Sort.by(orders));
    }

    public static void writePageable(Output output, Pageable request) {
        // Write pageNo
        output.writeInt(request.getPageNumber());
        // Write pageSize
        output.writeInt(request.getPageSize());
        // Write sort
        output.writeInt((int) request.getSort().stream().count());
        Iterator<Sort.Order> orderIterator = request.getSort().stream().iterator();
        while (orderIterator.hasNext()) {
            Sort.Order order = orderIterator.next();
            output.writeString(order.getDirection().name());
            output.writeString(order.getProperty());
            output.writeString(order.getNullHandling().name());
        }
    }

    public static List<Sort.Order> readOrders(Input input, int orderSize) {
        List<Sort.Order> orders = new ArrayList<>(orderSize);
        for (int i = 0; i < orderSize; i++) {
            // Read Direction
            Sort.Direction direction = Sort.Direction.fromString(input.readString());
            // Read property
            String property = input.readString();
            // Read NullHandling
            Sort.NullHandling nullHandling = Sort.NullHandling.valueOf(input.readString());

            orders.add(new Sort.Order(direction, property, nullHandling));
        }
        return orders;
    }
}
