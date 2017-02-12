/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.dbus.databind.binder;

import at.yawk.dbus.protocol.object.BasicObject;
import at.yawk.dbus.protocol.object.DbusObject;
import at.yawk.dbus.protocol.object.ObjectPathObject;
import at.yawk.dbus.protocol.object.VariantObject;
import at.yawk.dbus.protocol.type.BasicType;
import at.yawk.dbus.protocol.type.TypeDefinition;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * @author yawkat
 */
public class PrimitiveBinderFactory implements BinderFactory {
    @Getter
    private static final PrimitiveBinderFactory instance = new PrimitiveBinderFactory();

    private static final Map<Class<?>, Binder<?>> typeBinders = new HashMap<>();
    private static final Map<TypeDefinition, Binder<?>> defaultBinders = new HashMap<>();

    static {
        Binder<String> stringBinder = Binder.of(
                BasicType.STRING, DbusObject::stringValue, BasicObject::createString);

        Binder<Boolean> booleanBinder = Binder.of(
                BasicType.BOOLEAN, DbusObject::booleanValue, BasicObject::createBoolean);
        Binder<Byte> byteBinder = Binder.of(
                BasicType.BYTE, DbusObject::byteValue, BasicObject::createByte);
        Binder<Short> shortBinder = Binder.of(
                BasicType.INT16, DbusObject::shortValue, BasicObject::createInt16);
        Binder<Short> unsignedShortBinder = Binder.of(
                BasicType.UINT16, DbusObject::shortValue, BasicObject::createUint16);
        Binder<Integer> integerBinder = Binder.of(
                BasicType.INT32, DbusObject::intValue, BasicObject::createInt32);
        Binder<Integer> unsignedIntegerBinder = Binder.of(
                BasicType.UINT32, DbusObject::intValue, BasicObject::createUint32);
        Binder<Long> longBinder = Binder.of(
                BasicType.INT64, DbusObject::longValue, BasicObject::createInt64);
        Binder<Long> unsignedLongBinder = Binder.of(
                BasicType.UINT64, DbusObject::longValue, BasicObject::createUint64);
        Binder<Float> floatBinder = Binder.<Float>of(
                BasicType.DOUBLE, obj -> (float) obj.doubleValue(), BasicObject::createDouble);
        Binder<Double> doubleBinder = Binder.of(
                BasicType.DOUBLE, DbusObject::doubleValue, BasicObject::createDouble);
        Binder<DbusObject> variantBinder = Binder.of(
                BasicType.VARIANT, DbusObject::getValue, VariantObject::create);
        Binder<String> objectPathBinder = Binder.of(
                BasicType.OBJECT_PATH, DbusObject::stringValue, ObjectPathObject::create);

        typeBinders.put(String.class, stringBinder);
        typeBinders.put(boolean.class, booleanBinder);
        typeBinders.put(Boolean.class, booleanBinder);
        typeBinders.put(byte.class, byteBinder);
        typeBinders.put(Byte.class, byteBinder);
        typeBinders.put(short.class, shortBinder);
        typeBinders.put(Short.class, shortBinder);
        typeBinders.put(int.class, integerBinder);
        typeBinders.put(Integer.class, integerBinder);
        typeBinders.put(long.class, longBinder);
        typeBinders.put(Long.class, longBinder);
        typeBinders.put(float.class, floatBinder);
        typeBinders.put(Float.class, floatBinder);
        typeBinders.put(double.class, doubleBinder);
        typeBinders.put(Double.class, doubleBinder);
        typeBinders.put(DbusObject.class, variantBinder);

        for (Binder<?> binder : new Binder[]{
                stringBinder,
                booleanBinder,
                byteBinder,
                shortBinder,
                unsignedShortBinder,
                integerBinder,
                unsignedIntegerBinder,
                longBinder,
                unsignedLongBinder,
                floatBinder,
                doubleBinder,
                objectPathBinder
        }) {
            defaultBinders.put(binder.getType(), binder);
        }
    }

    private PrimitiveBinderFactory() {}

    @Override
    public Binder<?> getBinder(BinderFactoryContext ctx, Type type) {
        //noinspection SuspiciousMethodCalls
        return typeBinders.get(type);
    }

    @Nullable
    @Override
    public Binder<?> getDefaultDecodeBinder(BinderFactoryContext ctx, TypeDefinition typeDefinition) {
        return defaultBinders.get(typeDefinition);
    }
}
