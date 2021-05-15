package org.infinity.rpc.demoserver.utils.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

public class PageImplSerializer extends Serializer<PageImpl> {
    @Override
    public void write(Kryo kryo, Output output, PageImpl page) {
        // Write contents
        output.writeInt(CollectionUtils.isNotEmpty(page.getContent()) ? page.getContent().size() : 0);

        if (CollectionUtils.isNotEmpty(page.getContent())) {
            // Write list element type
            for (Object item : page.getContent()) {
                kryo.writeClassAndObject(output, item);
            }
        }

        // Write Pageable
        kryo.writeObjectOrNull(output, page.getPageable(), Pageable.class);

        // Write total
        output.writeLong(page.getTotalElements());
    }

    @Override
    public PageImpl read(Kryo kryo, Input input, Class<PageImpl> type) {
        // Read contents
        int contentSize = input.readInt();
        List contents = new ArrayList<>(contentSize);
        if (contentSize != 0) {
            for (int i = 0; i < contentSize; i++) {
                contents.add(kryo.readClassAndObject(input));
            }
        }

        // Read Pageable
        Pageable pageable = kryo.readObjectOrNull(input, Pageable.class);
        return new PageImpl(contents, pageable, input.readLong());
    }
}