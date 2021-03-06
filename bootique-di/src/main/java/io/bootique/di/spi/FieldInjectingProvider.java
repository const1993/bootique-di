package io.bootique.di.spi;

import io.bootique.di.Key;
import io.bootique.di.TypeLiteral;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.Predicate;

class FieldInjectingProvider<T> extends MemberInjectingProvider<T> {

    FieldInjectingProvider(Provider<T> delegate, DefaultInjector injector) {
        super(delegate, injector);
    }

    @Override
    protected void injectMembers(T object, Class<?> type) {

        // bail on recursion stop condition
        if (type == Object.class) {
            return;
        }

        injectMembers(object, type.getSuperclass());

        Predicate<AccessibleObject> injectPredicate = injector.getPredicates().getInjectPredicate();

        for (Field field : type.getDeclaredFields()) {
            if(Modifier.isStatic(field.getModifiers())) {
                // skip static fields completely
                continue;
            }

            if (injectPredicate.test(field)) {
                injectMember(object, field, getQualifier(field));
            }
        }
    }

    private void injectMember(Object object, Field field, Annotation bindingAnnotation) {

        injector.trace(() -> "Injecting field '" + field.getName() + "' of class " + field.getDeclaringClass().getName());
        Object value = value(field, bindingAnnotation);

        field.setAccessible(true);
        try {
            field.set(object, value);
        } catch (Exception e) {
            injector.throwException("Error injecting into field %s.%s of type %s"
                    , e, field.getDeclaringClass().getName(), field.getName(), field.getType().getName());
        }
    }

    protected Object value(Field field, Annotation bindingAnnotation) {

        Class<?> fieldType = field.getType();
        if (injector.getPredicates().isProviderType(fieldType)) {
            Type parameterType = DIUtil.getGenericParameterType(field.getGenericType());

            if (parameterType == null) {
                injector.throwException("Provider field %s.%s must be parameterized to be usable for injection"
                        , field.getDeclaringClass().getName(), field.getName());
            }

            return injector.getProvider(Key.get(TypeLiteral.of(parameterType), bindingAnnotation));
        } else {
            Key<?> key = Key.get(TypeLiteral.of(field.getGenericType()), bindingAnnotation);
            InjectionStack stack = injector.getInjectionStack();
            stack.push(key);
            try {
                return injector.getInstance(key);
            } finally {
                stack.pop();
            }
        }
    }

    @Override
    public String getName() {
        return "field injecting provider";
    }
}
