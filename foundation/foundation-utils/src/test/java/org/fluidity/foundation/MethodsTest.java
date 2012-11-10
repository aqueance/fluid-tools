/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.foundation;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class MethodsTest {

    @Test
    public void testReturnTypes() throws Exception {
        final Method[] methods = Methods.get(VariousMethods.class, new Methods.Invoker<VariousMethods>() {
            public void invoke(final VariousMethods capture) throws Exception {
                capture.voidMethod();
                capture.byteMethod();
                capture.shortMethod();
                capture.intMethod();
                capture.longMethod();
                capture.floatMethod();
                capture.doubleMethod();
                capture.booleanMethod();
                capture.charMethod();
                capture.VoidMethod();
                capture.ObjectMethod();
                capture.StringMethod();
                capture.FloatMethod();
            }
        });

        assert methods.length == 13 : Arrays.toString(methods);
        assert "voidMethod".equals(methods[0].getName()) : methods[0];
        assert "byteMethod".equals(methods[1].getName()) : methods[1];
        assert "shortMethod".equals(methods[2].getName()) : methods[2];
        assert "intMethod".equals(methods[3].getName()) : methods[3];
        assert "longMethod".equals(methods[4].getName()) : methods[4];
        assert "floatMethod".equals(methods[5].getName()) : methods[5];
        assert "doubleMethod".equals(methods[6].getName()) : methods[6];
        assert "booleanMethod".equals(methods[7].getName()) : methods[7];
        assert "charMethod".equals(methods[8].getName()) : methods[8];
        assert "VoidMethod".equals(methods[9].getName()) : methods[9];
        assert "ObjectMethod".equals(methods[10].getName()) : methods[10];
        assert "StringMethod".equals(methods[11].getName()) : methods[11];
        assert "FloatMethod".equals(methods[12].getName()) : methods[12];
    }

    interface VariousMethods {
        void voidMethod();
        byte byteMethod();
        short shortMethod();
        int intMethod();
        long longMethod();
        float floatMethod();
        double doubleMethod();
        boolean booleanMethod();
        char charMethod();
        Void VoidMethod();
        Object ObjectMethod();
        String StringMethod();
        Float FloatMethod();
    }
}
