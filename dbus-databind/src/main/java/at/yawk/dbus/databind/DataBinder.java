package at.yawk.dbus.databind;

import at.yawk.dbus.databind.annotation.Transformer;
import at.yawk.dbus.databind.binder.*;
import at.yawk.dbus.protocol.type.TypeDefinition;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yawkat
 */
public class DataBinder implements BinderFactoryContext {
    private final Map<Class<? extends Annotation>, Optional<AnnotationBinderTransformer<?>>> transformerCache =
            new ConcurrentHashMap<>();
    private final List<BinderFactory> binderFactories = new ArrayList<>();
    private final Map<Type, Binder<?>> binderCache = new ConcurrentHashMap<>();
    private final Map<TypeDefinition, Binder<?>> defaultBinderCache = new ConcurrentHashMap<>();

    {
        binderFactories.add(PrimitiveBinderFactory.getInstance());
        binderFactories.add(ArrayBinderFactory.getInstance());
        binderFactories.add(DictBinderFactory.getInstance());
        binderFactories.add(ObjectBinderFactory.getInstance());
    }

    private <A extends Annotation> Optional<AnnotationBinderTransformer<?>> getBinderTransformer(
            Class<A> annotationClass) {
        return transformerCache.computeIfAbsent(
                annotationClass,
                ac -> {
                    Transformer transformerAnnotation = ac.getAnnotation(Transformer.class);
                    if (transformerAnnotation == null) { return Optional.empty(); }
                    try {
                        return Optional.of(transformerAnnotation.value().newInstance());
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @SuppressWarnings("unchecked")
    private Binder<?> applyBinderTransforms(Binder<?> binder, AnnotatedElement element) {
        for (Annotation annotation : element.getAnnotations()) {
            Optional<AnnotationBinderTransformer<?>> transformer = getBinderTransformer(annotation.getClass());
            if (transformer.isPresent()) {
                binder = ((AnnotationBinderTransformer) transformer.get()).wrap(annotation, binder);
            }
        }
        return binder;
    }

    public void addBinderFactory(BinderFactory factory) {
        binderFactories.add(0, factory);
    }

    public Binder<?> getBinder(Type type, AnnotatedElement metaElement) {
        Binder<?> baseBinder = getBinder(type);
        return applyBinderTransforms(baseBinder, metaElement);
    }

    @Override
    public Binder<?> getBinder(Type type) {
        return binderCache.computeIfAbsent(type, t -> {
            for (BinderFactory factory : binderFactories) {
                Binder<?> binder = factory.getBinder(this, type);
                if (binder != null) {
                    return binder;
                }
            }
            throw new UnsupportedOperationException("Cannot bind " + type);
        });
    }

    @Override
    public Binder<?> getDefaultBinder(TypeDefinition typeDefinition) {
        return defaultBinderCache.computeIfAbsent(typeDefinition, t -> {
            for (BinderFactory factory : binderFactories) {
                Binder<?> binder = factory.getDefaultBinder(this, typeDefinition);
                if (binder != null) {
                    return binder;
                }
            }
            throw new UnsupportedOperationException("Cannot bind " + typeDefinition);
        });
    }
}