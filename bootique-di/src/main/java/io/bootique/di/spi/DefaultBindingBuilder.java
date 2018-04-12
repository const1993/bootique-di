package io.bootique.di.spi;

import io.bootique.di.BindingBuilder;
import io.bootique.di.DIRuntimeException;
import io.bootique.di.Key;
import io.bootique.di.Scope;

import javax.inject.Provider;
import javax.inject.Singleton;

class DefaultBindingBuilder<T> implements BindingBuilder<T> {

    protected DefaultInjector injector;
    protected Key<T> bindingKey;

    DefaultBindingBuilder(Key<T> bindingKey, DefaultInjector injector) {
        this.injector = injector;
        this.bindingKey = bindingKey;
    }

    @Override
    public BindingBuilder<T> to(Class<? extends T> implementation) throws DIRuntimeException {
        Provider<T> provider0 = new ConstructorInjectingProvider<>(implementation, injector);
        Provider<T> provider1 = new FieldInjectingProvider<>(provider0, injector);
        Provider<T> provider2 = new MethodInjectingProvider<>(provider1, injector);

        injector.putBinding(bindingKey, provider2);
        if(implementation.getAnnotation(Singleton.class) != null) {
            injector.changeBindingScope(bindingKey, injector.getSingletonScope());
        }

        return this;
    }

    @Override
    public BindingBuilder<T> to(Key<? extends T> key) throws DIRuntimeException {
        return toProviderInstance(() -> injector.getProvider(key).get());
    }

    @Override
    public BindingBuilder<T> toInstance(T instance) throws DIRuntimeException {
        Provider<T> provider0 = new InstanceProvider<>(instance);
        Provider<T> provider1 = new FieldInjectingProvider<>(provider0, injector);
        Provider<T> provider2 = new MethodInjectingProvider<>(provider1, injector);

        injector.putBinding(bindingKey, provider2);
        injector.changeBindingScope(bindingKey, injector.getSingletonScope());

        return this;
    }

    @Override
    public BindingBuilder<T> toProvider(Class<? extends Provider<? extends T>> providerType) {
        Provider<Provider<? extends T>> provider0 = new ConstructorInjectingProvider<>(providerType, injector);
        Provider<Provider<? extends T>> provider1 = new FieldInjectingProvider<>(provider0, injector);
        Provider<Provider<? extends T>> provider2 = new MethodInjectingProvider<>(provider1, injector);

        Provider<T> provider3 = new CustomProvidersProvider<>(provider2);
        Provider<T> provider4 = new FieldInjectingProvider<>(provider3, injector);
        Provider<T> provider5 = new MethodInjectingProvider<>(provider4, injector);

        injector.putBinding(bindingKey, provider5);
        return this;
    }

    @Override
    public BindingBuilder<T> toProviderInstance(Provider<? extends T> provider) {
        Provider<Provider<? extends T>> provider0 = new InstanceProvider<>(provider);
        Provider<Provider<? extends T>> provider1 = new FieldInjectingProvider<>(provider0, injector);
        Provider<Provider<? extends T>> provider2 = new MethodInjectingProvider<>(provider1, injector);

        Provider<T> provider3 = new CustomProvidersProvider<>(provider2);
        Provider<T> provider4 = new FieldInjectingProvider<>(provider3, injector);
        Provider<T> provider5 = new MethodInjectingProvider<>(provider4, injector);

        injector.putBinding(bindingKey, provider5);
        return this;
    }

    @Override
    public void in(Scope scope) {
        injector.changeBindingScope(bindingKey, scope);
    }

    @Override
    public void withoutScope() {
        in(injector.getNoScope());
    }

    @Override
    public void inSingletonScope() {
        in(injector.getSingletonScope());
    }
}
