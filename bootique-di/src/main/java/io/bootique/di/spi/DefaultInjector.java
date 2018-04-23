package io.bootique.di.spi;

import io.bootique.di.BindingBuilder;
import io.bootique.di.DIRuntimeException;
import io.bootique.di.InjectionTraceElement;
import io.bootique.di.Injector;
import io.bootique.di.Key;
import io.bootique.di.Module;
import io.bootique.di.Scope;

import javax.inject.Provider;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A default implementations of a DI injector.
 */
public class DefaultInjector implements Injector {

    private final DefaultScope singletonScope;
    private final Scope noScope;

    private final DefaultBinder binder;
    private final Map<Key<?>, Binding<?>> bindings;
    private final Map<Key<?>, Decoration<?>> decorations;
    private final ProvidesHandler providesHandler;
    private final InjectionStack injectionStack;
    private final InjectionTrace injectionTrace;
    private final Scope defaultScope;
    private final InjectorPredicates predicates;

    private final boolean allowDynamicBinding;
    private final boolean allowOverride;
    private final boolean allowMethodInjection;
    private final boolean injectionTraceEnabled;

    private volatile boolean isShutdown;

    DefaultInjector(Module... modules) {
        this(Collections.emptySet(), new InjectorPredicates(), modules);
    }

    public DefaultInjector(Set<Options> options, InjectorPredicates predicates, Module... modules) {
        this.predicates = predicates;

        this.singletonScope = new DefaultScope();
        this.noScope = NoScope.INSTANCE;
        if(options.contains(Options.NO_SCOPE_BY_DEFAULT)) {
            this.defaultScope = noScope;
        } else {
            this.defaultScope = singletonScope;
        }

        this.allowOverride = !options.contains(Options.DECLARED_OVERRIDE_ONLY);
        this.allowDynamicBinding = options.contains(Options.ENABLE_DYNAMIC_BINDINGS);
        this.allowMethodInjection = options.contains(Options.ENABLE_METHOD_INJECTION);
        this.injectionTraceEnabled = !options.contains(Options.DISABLE_TRACE);

        this.bindings = new ConcurrentHashMap<>();
        this.decorations = new ConcurrentHashMap<>();
        this.injectionStack = new InjectionStack(this);
        this.injectionTrace = injectionTraceEnabled ? new InjectionTrace() : null;
        this.providesHandler = new ProvidesHandler(this);
        this.binder = new DefaultBinder(this);

        // bind self for injector injection...
        binder.bind(Injector.class).toInstance(this);

        // bind modules
        if (modules != null && modules.length > 0) {
            for (Module module : modules) {
                module.configure(binder);
                providesHandler.bindingsFromAnnotatedMethods(module).forEach(p -> p.bind(this));
            }
        }

        applyDecorators();
    }

    InjectionStack getInjectionStack() {
        return injectionStack;
    }

    DefaultBinder getBinder() {
        return binder;
    }

    ProvidesHandler getProvidesHandler() {
        return providesHandler;
    }

    InjectorPredicates getPredicates() {
        return predicates;
    }

    @SuppressWarnings("unchecked")
    <T> Binding<T> getBinding(Key<T> key) {
        if(isShutdown) {
            throwException("Injector is shutdown");
        }
        // may return null - this is intentionally allowed in this non-public method
        return (Binding<T>) bindings.get(Objects.requireNonNull(key, "Null key"));
    }

    <T> void putBinding(Key<T> bindingKey, Provider<T> provider) {
        putBinding(bindingKey, new Binding<>(bindingKey, wrapProvider(bindingKey, provider), defaultScope, false));
    }

    <T> void putOptionalBinding(Key<T> bindingKey, Provider<T> provider) {
        putBinding(bindingKey, new Binding<>(bindingKey, wrapProvider(bindingKey, provider), defaultScope, true));
    }

    <T> void putBinding(Key<T> bindingKey, Binding<T> binding) {
        if(isShutdown) {
            throwException("Injector is shutdown");
        }
        Binding<?> oldBinding = bindings.put(bindingKey, binding);
        if(!canOverride(oldBinding, binding)) {
            throwException("Unable to override key %s. It is final and override is disabled.", bindingKey);
        }
    }

    /**
     * <ul>
     *     <li> Can always override optional bindings
     *     <li> Can override if new binding marked as override
     *     <li> Can always override if Injector configured with overrides enabled
     * </ul>
     *
     * @param oldBinding existing binding (or null if absent) to override
     * @param withBinding new binding that wants to override existing one
     *
     * @return can binding be overridden with new one
     */
    private boolean canOverride(Binding<?> oldBinding, Binding<?> withBinding) {
        // binding provider can be null if it is incomplete (e.g. binder.bind(MyClass.class);)
        return oldBinding == null || oldBinding.getOriginal() == null
                || oldBinding.isOptional()
                || allowOverride;
    }

    @SuppressWarnings("unchecked")
    private <T> Decoration<T> getDecoration(Key<T> bindingKey) {
        if(isShutdown) {
            throwException("Injector is shutdown");
        }
        return (Decoration<T>)decorations.computeIfAbsent(bindingKey, bk -> new Decoration<>());
    }

    <T> void putDecorationAfter(Key<T> bindingKey, DecoratorProvider<T> decoratorProvider) {
        getDecoration(bindingKey).after(decoratorProvider);
    }

    <T> void putDecorationBefore(Key<T> bindingKey, DecoratorProvider<T> decoratorProvider) {
        getDecoration(bindingKey).before(decoratorProvider);
    }

    <T> void changeBindingScope(Key<T> bindingKey, Scope scope) {
        if (scope == null) {
            scope = noScope;
        }

        Binding<?> binding = bindings.get(bindingKey);
        if (binding == null) {
            throwException("No existing binding for key " + bindingKey);
            return;
        }

        binding.changeScope(scope);
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        return getInstance(Key.get(type));
    }

    @Override
    public <T> T getInstance(Key<T> key) {
        return getProvider(key).get();
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> type) {
        return getProvider(Key.get(type));
    }

    @Override
    public <T> Provider<T> getProvider(Key<T> key) {
        Binding<T> binding = getBinding(key);
        if (binding == null || binding.getOriginal() == null) {
            binding = createDynamicBinding(key, binding);
        }

        return predicates.wrapProvider(binding.getScoped());
    }

    @SuppressWarnings("unchecked")
    private <T> Binding<T> createDynamicBinding(Key<T> key, Binding<T> binding) {
        if(binding == null && !allowDynamicBinding) {
            throwException("DI container has no binding for key %s and dynamic bindings are disabled.", key);
        }
        // create new binding
        BindingBuilder<T> builder = binder.bind(key).to((Class)key.getType().getRawType());
        // if binding was already declared use it's scope
        if(binding != null) {
            builder.in(binding.getScope());
        }
        return getBinding(key);
    }

    @Override
    public void injectMembers(Object object) {
        Provider<Object> provider = new InstanceProvider<>(object);
        provider = new FieldInjectingProvider<>(provider, this);
        if(allowMethodInjection) {
            provider = new MethodInjectingProvider<>(provider, this);
        }
        @SuppressWarnings("unchecked")
        Class<Object> objectClass = (Class<Object>)object.getClass();
        wrapProvider(Key.get(objectClass), provider).get();
    }

    @Override
    public synchronized void shutdown() {
        if(isShutdown) {
            return;
        }
        isShutdown = true;
        singletonScope.shutdown();
        bindings.clear();
        decorations.clear();
        injectionStack.reset();
    }

    DefaultScope getSingletonScope() {
        return singletonScope;
    }

    Scope getDefaultScope() {
        return defaultScope;
    }

    Scope getNoScope() {
        return noScope;
    }

    boolean isMethodInjectionEnabled() {
        return allowMethodInjection;
    }

    boolean isInjectionTraceEnabled() {
        return injectionTraceEnabled;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applyDecorators() {
        for (Entry<Key<?>, Decoration<?>> e : decorations.entrySet()) {

            Binding b = bindings.get(e.getKey());
            if (b == null) {
                // TODO: print warning - decorator of a non-existing service..
                continue;
            }

            b.decorate(this, e.getValue());
        }
    }

    public enum Options {
        NO_SCOPE_BY_DEFAULT,
        DECLARED_OVERRIDE_ONLY,
        ENABLE_DYNAMIC_BINDINGS,
        ENABLE_METHOD_INJECTION,
        DISABLE_TRACE
    }

    /**
     * Wraps provider in traceable provider if trace is enabled
     */
    <T> Provider<T> wrapProvider(Key<T> key, Provider<T> provider) {
        if(!injectionTraceEnabled || provider == null) {
            return provider;
        }

        return new TraceableProvider<>(key, provider, this);
    }

    /**
     * @param message trace message format
     * @param args for message formatting
     */
    void trace(String message, Object... args) {
        if (injectionTraceEnabled) {
            injectionTrace.updateMessage(String.format(message, args));
        }
    }

    /**
     * Push currently resolving key into trace stack
     * @param key that is resolving
     */
    void tracePush(Key<?> key) {
        if (injectionTraceEnabled) {
            injectionTrace.push(key);
        }
    }

    /**
     * Pop key out of trace stack
     */
    void tracePop() {
        if (injectionTraceEnabled) {
            injectionTrace.pop();
        }
    }

    /**
     * Formats and throws DI exception with injection trace attached to it.
     * <p>
     * Can be used anywhere like {@code return throwException("some message");}
     *
     * @param message message
     * @param args message format arguments
     * @param <T> generic type to return
     * @return nothing, alwasy throws
     * @throws DIRuntimeException throws DI exception always
     */
    <T> T throwException(String message, Object... args) throws DIRuntimeException {
        throw setTrace(new DIRuntimeException(message, args));
    }

    /**
     * Formats and throws DI exception with injection trace attached to it.
     * <p>
     * Can be used anywhere like {@code return throwException("some message", cause);}
     * <p>
     * This method will try to unwrap {@code cause}
     * <ul>
     *     <li>if it is another {@link DIRuntimeException} it will be rethrown as is
     *     <li>if it is {@link InvocationTargetException} it cause will be used as cause for DI exception
     *     <li>otherwise cause will be used as is
     * </ul>
     *
     * @param message message
     * @param cause underlying cause of this exception
     * @param args message format arguments
     * @param <T> generic type to return
     * @return nothing, always throws
     * @throws DIRuntimeException throws DI exception always
     */
    <T> T throwException(String message, Throwable cause, Object... args) throws DIRuntimeException {
        if (cause instanceof InvocationTargetException && cause.getCause() != null) {
            // if reflection method call thrown an exception unwrap and use it as a cause
            cause = cause.getCause();
        }

        // TODO: is assumption bellow always correct?
        // If it was other DI exception, use it. It will better point to the actual problem.
        if (cause instanceof DIRuntimeException) {
            throw (DIRuntimeException) cause;
        }

        throw setTrace(new DIRuntimeException(message, cause, args));
    }


    /**
     * Set trace (if any) to exception
     * @param ex exception
     * @return ex
     */
    private DIRuntimeException setTrace(DIRuntimeException ex) {
        if (!injectionTraceEnabled) {
            return ex;
        }

        InjectionTraceElement[] traceElements = new InjectionTraceElement[injectionTrace.size()];
        InjectionTraceElement element;
        int i = 0;
        while((element = injectionTrace.pop()) != null) {
            traceElements[i++] = element;
        }

        ex.setInjectionTrace(traceElements);
        return ex;
    }

}
