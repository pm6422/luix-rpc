package com.luixtech.luixrpc.core.codec.impl;

import com.luixtech.luixrpc.core.exception.impl.RpcInvocationException;

import java.nio.ByteBuffer;

public class GrowableByteBuffer {

    public static int encodeZigZag32(int value) {
        return (value << 1) ^ (value >> 31);
    }

    public static long encodeZigZag64(long value) {
        return (value << 1) ^ (value >> 63);
    }

    public static int decodeZigZag32(int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    public static long decodeZigZag64(long n) {
        return (n >>> 1) ^ -(n & 1);
    }

    private ByteBuffer buf;

    public GrowableByteBuffer(int initSize) {
        this.buf = ByteBuffer.allocate(initSize);
    }

    public GrowableByteBuffer(ByteBuffer buf) {
        this.buf = buf;
    }

    public void put(byte b) {
        ensureBufferEnough(1);
        buf.put(b);
    }

    public void put(int index, byte b) {
        buf.put(index, b);
    }

    public void put(byte[] b) {
        ensureBufferEnough(b.length);
        buf.put(b);
    }

    public void putShort(short value) {
        ensureBufferEnough(2);
        buf.putShort(value);
    }

    public void putShort(int index, short value) {
        buf.putShort(index, value);
    }

    public void putInt(int value) {
        ensureBufferEnough(4);
        buf.putInt(value);
    }

    public void putInt(int index, int value) {
        buf.putInt(index, value);
    }

    public void putLong(long value) {
        ensureBufferEnough(8);
        buf.putLong(value);
    }

    public void putLong(int index, long value) {
        buf.putLong(index, value);
    }

    public void putFloat(float value) {
        ensureBufferEnough(4);
        buf.putFloat(value);
    }

    public void putFloat(int index, float value) {
        buf.putFloat(index, value);
    }

    public void putDouble(double value) {
        ensureBufferEnough(8);
        buf.putDouble(value);
    }

    public void putDouble(int index, double value) {
        buf.putDouble(index, value);
    }

    public int putZigzag32(int value) {
        return putVarInt(encodeZigZag32(value));
    }

    public int putZigzag64(long value) {
        return putVarInt(encodeZigZag64(value));
    }

    public int putVarInt(long value) {
        int count = 0;
        while (true) {
            count++;
            if ((value & ~0x7fL) == 0) {
                put((byte) value);
                break;
            } else {
                put((byte) ((value & 0x7f) | 0x80));
                value >>>= 7;
            }
        }
        return count;
    }

    public byte get() {
        return buf.get();
    }

    public byte get(int index) {
        return buf.get(index);
    }

    public void get(byte[] dst) {
        buf.get(dst);
    }

    public short getShort() {
        return buf.getShort();
    }

    public short getShort(int index) {
        return buf.getShort(index);
    }

    public int getInt() {
        return buf.getInt();
    }

    public int getInt(int index) {
        return buf.getInt(index);
    }

    public long getLong() {
        return buf.getLong();
    }

    public long getLong(int index) {
        return buf.getLong(index);
    }

    public float getFloat() {
        return buf.getFloat();
    }

    public float getFloat(int index) {
        return buf.getFloat(index);
    }

    public double getDouble() {
        return buf.getDouble();
    }

    public double getDouble(int index) {
        return buf.getDouble(index);
    }

    public int getZigZag32() {
        return decodeZigZag32((int) getVarInt());
    }

    public long getZigZag64() {
        return decodeZigZag64(getVarInt());
    }

    public long getVarInt() {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            final byte b = buf.get();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new RpcInvocationException("Integer overflow");
    }

    public void flip() {
        buf.flip();
    }

    public int position() {
        return buf.position();
    }

    public void position(int newPosition) {
        ensureBufferEnough(newPosition - buf.position());
        buf.position(newPosition);
    }

    public int limit() {
        return buf.limit();
    }

    public void limit(int newLimit) {
        buf.limit(newLimit);
    }

    public int capacity() {
        return buf.capacity();
    }

    public int remaining() {
        return buf.remaining();
    }

    public void clear() {
        buf.clear();
    }

    private ByteBuffer grow(int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put(buf.array());
        buffer.position(buf.position());
        return buffer;
    }

    private void ensureBufferEnough(int need) {
        int expandSize = buf.position() + need;
        if (buf.capacity() < expandSize) {
            int size = buf.capacity() * 2;
            while (size < expandSize) {
                size = size * 2;
            }
            buf = grow(size);
        }
    }
}
