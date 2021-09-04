package org.infinity.luix.spring.enhancement.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SortSerializer extends Serializer<Sort> {

    @Override
    public void write(Kryo kryo, Output output, Sort sort) {
        output.writeInt((int) sort.stream().count());
        Iterator<Sort.Order> orderIterator = sort.stream().iterator();
        while (orderIterator.hasNext()) {
            Sort.Order order = orderIterator.next();
            output.writeString(order.getDirection().name());
            output.writeString(order.getProperty());
            output.writeString(order.getNullHandling().name());
        }
    }

    @Override
    public Sort read(Kryo kryo, Input input, Class<? extends Sort> type) {
        int sortSize = input.readInt();
        List<Sort.Order> orders = new ArrayList<>(sortSize);
        for (int i = 0; i < sortSize; i++) {
            // Read Direction
            Sort.Direction direction = Sort.Direction.fromString(input.readString());
            // Read property
            String property = input.readString();
            // Read NullHandling
            Sort.NullHandling nullHandling = Sort.NullHandling.valueOf(input.readString());

            orders.add(new Sort.Order(direction, property, nullHandling));
        }
        return sortSize == 0 ? Sort.unsorted() : Sort.by(orders);
    }
}
