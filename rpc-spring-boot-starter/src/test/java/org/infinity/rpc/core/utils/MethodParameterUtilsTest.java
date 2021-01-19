/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.infinity.rpc.core.utils;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.infinity.rpc.core.utils.MethodParameterUtils.VOID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MethodParameterUtilsTest {

    @Test
    public void testGetMethodParamTypeString() {
        Method method;
        try {
            method = ClassA.class.getMethod("get");
            assertEquals(VOID, MethodParameterUtils.getMethodParameters(method));
            method = ClassA.class.getMethod("getInt", int.class);
            assertEquals("int", MethodParameterUtils.getMethodParameters(method));
            method = ClassA.class.getMethod("getIntLong", int.class, long.class);
            assertEquals("int,long", MethodParameterUtils.getMethodParameters(method));
            method = ClassA.class.getMethod("getLongWrapper", Long.class);
            assertEquals("java.lang.Long", MethodParameterUtils.getMethodParameters(method));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testReflect() {
        Method method;
        try {
            method = ClassA.class.getMethod("get");
            assertEquals("get(void)", MethodParameterUtils.getMethodSignature(method));
            method = ClassA.class.getMethod("getByte", byte.class);
            assertEquals("getByte(byte)", MethodParameterUtils.getMethodSignature(method));
            method = ClassA.class.getMethod("getList", List.class);
            assertEquals("getList(java.util.List)", MethodParameterUtils.getMethodSignature(method));
            method = ClassA.class.getMethod("getMap", Map.class);
            assertEquals("getMap(java.util.Map)", MethodParameterUtils.getMethodSignature(method));
            method = ClassA.class.getMethod("getStringArray", String[].class);
            assertEquals("getStringArray(java.lang.String[])", MethodParameterUtils.getMethodSignature(method));
            method = ClassA.class.getMethod("getIntArray", int[].class);
            assertEquals("getIntArray(int[])", MethodParameterUtils.getMethodSignature(method));
        } catch (Exception e) {
            fail();
        }
    }
}


class ClassA {
    public void get() {
    }

    public int getInt(int param) {
        return param;
    }

    public int[] getIntArray(int[] param) {
        return param;
    }

    public byte getByte(byte param) {
        return param;
    }

    public byte[] getByteArray(byte[] param) {
        return param;
    }

    public String getString(String param) {
        return param;
    }

    public String[] getStringArray(String[] param) {
        return param;
    }

    public List<Object> getList(List<Object> param) {
        return param;
    }

    public Map<Object, Object> getMap(Map<Object, Object> param) {
        return param;
    }

    public int getIntLong(int i, long j) {
        return i;
    }

    public long getLongWrapper(Long l) {
        return l;
    }
}
