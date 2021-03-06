package io.bootique.di.mock;

import javax.inject.Inject;
import javax.inject.Named;

public class MockImplementation2_Named implements MockInterface2 {

    @Inject
    @Named("one")
    private MockInterface1 service;

    public String getAlteredName() {
        return "altered_" + service.getName();
    }

    public String getName() {
        return "MockImplementation2_NamedName";
    }
}
