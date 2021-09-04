package org.infinity.luix.spring.enhancement.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.data.mongodb.core.query.Criteria;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class CriteriaSerializer extends Serializer<Criteria> {

    private final Field notSetField;
    private final Field isValueField;

    public CriteriaSerializer() {
        try {
            notSetField = Criteria.class.getDeclaredField("NOT_SET");
            notSetField.setAccessible(true);
            isValueField = Criteria.class.getDeclaredField("isValue");
            isValueField.setAccessible(true);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(Kryo kryo, Output output, Criteria criteria) {
        kryo.getDefaultSerializer(Criteria.class).write(kryo, output, criteria);
    }

    /**
     * 修复这个序列化问题
     * if (!NOT_SET.equals(isValue)) {
     * queryCriteria.put(this.key, this.isValue);
     * queryCriteria.putAll(document);
     * } else {
     * queryCriteria.put(this.key, document);
     * }
     *
     * @param kryo  kryo
     * @param input input
     * @param type  type
     * @return criteria object
     */
    @Override
    public Criteria read(Kryo kryo, Input input, Class<? extends Criteria> type) {
        @SuppressWarnings("unchecked")
        Criteria criteria = (Criteria) kryo.getDefaultSerializer(Criteria.class).read(kryo, input, Criteria.class);
        List<String> collectionQuery = Arrays.asList("$in", "$nin", "$all");
        if (criteria.getCriteriaObject().keySet().stream().anyMatch(collectionQuery::contains)) {
            Object notSetVal = getNotSet(criteria);
            try {
                isValueField.set(criteria, notSetVal);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return criteria;
    }

    private Object getNotSet(final Criteria obj) {
        try {
            return notSetField.get(obj);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
