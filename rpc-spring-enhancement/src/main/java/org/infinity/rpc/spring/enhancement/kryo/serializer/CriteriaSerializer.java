package org.infinity.rpc.spring.enhancement.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.data.mongodb.core.query.Criteria;

public class CriteriaSerializer extends Serializer<Criteria> {

    @Override
    public void write(Kryo kryo, Output output, Criteria criteria) {
    }

    @Override
    public Criteria read(Kryo kryo, Input input, Class<? extends Criteria> type) {
        return null;
    }
}
