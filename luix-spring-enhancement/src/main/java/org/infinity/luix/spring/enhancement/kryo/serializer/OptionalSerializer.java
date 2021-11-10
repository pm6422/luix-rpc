package org.infinity.luix.spring.enhancement.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.Optional;

public class OptionalSerializer extends Serializer<Optional<?>> {

    @Override
    public void write(Kryo kryo, Output output, Optional<?> opt) {
        if (opt.isPresent()) {
            kryo.writeClass(output, opt.get().getClass());
            kryo.writeObject(output, opt.get());
        }
    }

    @Override
    public Optional<?> read(Kryo kryo, Input input, Class<? extends Optional<?>> type) {
        if (input.end()) {
            return Optional.empty();
        }
        Registration registration = kryo.readClass(input);
        Object o = kryo.readObject(input, registration.getType());
        return Optional.of(o);
    }
}
