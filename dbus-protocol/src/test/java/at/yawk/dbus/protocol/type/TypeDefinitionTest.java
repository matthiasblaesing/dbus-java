/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.dbus.protocol.type;

import java.util.Arrays;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author yawkat
 */
public class TypeDefinitionTest {
    @Test
    public void testSerializeBasic() {
        assertEquals(BasicType.INT32.serialize(), "i");
    }

    @Test
    public void testSerializeArray() {
        assertEquals(new ArrayTypeDefinition(BasicType.INT32).serialize(), "ai");
    }

    @Test
    public void testSerializeStruct() {
        assertEquals(new StructTypeDefinition(Arrays.asList(BasicType.INT32, BasicType.INT32)).serialize(), "(ii)");
    }

    @Test
    public void testSerializeDict() {
        assertEquals(new DictTypeDefinition(BasicType.INT32, BasicType.INT32).serialize(), "a{ii}");
    }

    @Test
    public void testLengthBasic() {
        assertEquals(BasicType.INT32.getLength(), 4);
    }
}