package org.infinity.rpc.spring.enhancement.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.data.mongodb.core.query.Query;

public class QuerySerializer extends Serializer<Query> {

    @Override
    public void write(Kryo kryo, Output output, Query sort) {
    }

    @Override
    public Query read(Kryo kryo, Input input, Class<? extends Query> type) {
        return null;
    }
}
