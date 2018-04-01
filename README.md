# bootique-di

## TODO:

Things to do to turn Cayenne DI into an adequate replacement of Guice in
Bootique:

* `List` vs `Set` bindings (we'd like to preserve a notion of order,
but still make it set-compatible... maybe support both?
* Allow binding generic types. `TypeLiteral` must be public.
* Support for provider methods in the modules.
* Replace individual constructor param injection with full constructor injection.
* Use `javax.inject` instead of own annotations (support for @Named, @Qualifier,
  @Singleton).
* Handling overrides (throw instead of ignore ... or maybe make it optional)
* Error handling - make sure we report multiple errors
* Optional Guice compatibility module - support for Guice annotations,
perhaps multi-binder API.