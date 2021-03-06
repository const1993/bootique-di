
package io.bootique.di.spi;

import io.bootique.di.Key;
import io.bootique.di.Module;
import io.bootique.di.TypeLiteral;
import io.bootique.di.mock.MockImplementation1;
import io.bootique.di.mock.MockImplementation1Alt;
import io.bootique.di.mock.MockImplementation1Alt2;
import io.bootique.di.mock.MockImplementation1_ListConfiguration;
import io.bootique.di.mock.MockImplementation1_ListConfigurationMock5;
import io.bootique.di.mock.MockImplementation1_MapConfiguration;
import io.bootique.di.mock.MockImplementation1_MapWithWildcards;
import io.bootique.di.mock.MockImplementation1_WithInjector;
import io.bootique.di.mock.MockImplementation2;
import io.bootique.di.mock.MockImplementation2Sub1;
import io.bootique.di.mock.MockImplementation2_ConstructorProvider;
import io.bootique.di.mock.MockImplementation2_ListConfiguration;
import io.bootique.di.mock.MockImplementation2_Named;
import io.bootique.di.mock.MockImplementation3;
import io.bootique.di.mock.MockImplementation4;
import io.bootique.di.mock.MockImplementation4Alt;
import io.bootique.di.mock.MockImplementation4Alt2;
import io.bootique.di.mock.MockImplementation5;
import io.bootique.di.mock.MockInterface1;
import io.bootique.di.mock.MockInterface2;
import io.bootique.di.mock.MockInterface3;
import io.bootique.di.mock.MockInterface4;
import io.bootique.di.mock.MockInterface5;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

public class DefaultInjectorInjectionTest {

    @Test
    public void testFieldInjection() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1.class);
            binder.bind(MockInterface2.class).to(MockImplementation2.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface2 service = injector.getInstance(MockInterface2.class);
        assertNotNull(service);
        assertEquals("altered_MyName", service.getAlteredName());
    }

    @Test
    public void testFieldInjection_Named() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1.class);
            binder.bind(Key.get(MockInterface1.class, "one")).to(MockImplementation1Alt.class);
            binder.bind(Key.get(MockInterface1.class, "two")).to(MockImplementation1Alt2.class);
            binder.bind(MockInterface2.class).to(MockImplementation2_Named.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface2 service = injector.getInstance(MockInterface2.class);
        assertNotNull(service);
        assertEquals("altered_alt", service.getAlteredName());
    }

    @Test
    public void testFieldInjectionSuperclass() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1.class);
            binder.bind(MockInterface2.class).to(MockImplementation2Sub1.class);
            binder.bind(MockInterface3.class).to(MockImplementation3.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface2 service = injector.getInstance(MockInterface2.class);
        assertNotNull(service);
        assertEquals("altered_MyName:XName", service.getAlteredName());
    }

    @Test
    public void testConstructorInjection() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1.class);
            binder.bind(MockInterface4.class).to(MockImplementation4.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface4 service = injector.getInstance(MockInterface4.class);
        assertNotNull(service);
        assertEquals("constructor_MyName", service.getName());
    }

    @Test
    public void testConstructorInjection_Named() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1.class);
            binder.bind(Key.get(MockInterface1.class, "one")).to(MockImplementation1Alt.class);
            binder.bind(Key.get(MockInterface1.class, "two")).to(MockImplementation1Alt2.class);
            binder.bind(MockInterface4.class).to(MockImplementation4Alt.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface4 service = injector.getInstance(MockInterface4.class);
        assertNotNull(service);
        assertEquals("constructor_alt2", service.getName());
    }

    @Test
    public void testConstructorInjection_Named_Mixed() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1.class);
            binder.bind(Key.get(MockInterface1.class, "one")).to(MockImplementation1Alt.class);
            binder.bind(Key.get(MockInterface1.class, "two")).to(MockImplementation1Alt2.class);
            binder.bind(MockInterface3.class).to(MockImplementation3.class);
            binder.bind(MockInterface4.class).to(MockImplementation4Alt2.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface4 service = injector.getInstance(MockInterface4.class);
        assertNotNull(service);
        assertEquals("constructor_alt2_XName", service.getName());
    }

    @Test
    public void testProviderInjection_Constructor() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1.class);
            binder.bind(MockInterface2.class).to(MockImplementation2_ConstructorProvider.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface2 service = injector.getInstance(MockInterface2.class);
        assertEquals("altered_MyName", service.getAlteredName());
    }

    @Test
    public void testMapInjection_Empty() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_MapConfiguration.class);

            // empty map must be still bound
            binder.bindMap(String.class, Object.class, "xyz");
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals("", service.getName());
    }

    @Test
    public void testMapInjection() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_MapConfiguration.class);
            binder.bindMap(String.class, Object.class,"xyz")
                    .put("x", "xvalue").put("y", "yvalue").put("x", "xvalue1");
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals(";x=xvalue1;y=yvalue", service.getName());
    }

    @Test
    public void mapWithWildcardInjection() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_MapWithWildcards.class);
            binder.bindMap(new TypeLiteral<String>(){}, new TypeLiteral<Class<?>>(){})
                    .put("x", String.class).put("y", Integer.class).put("z", Object.class);
        };
        DefaultInjector injector = new DefaultInjector(module);

        // This is example of how to deal with wildcards:
        Map<String, Class<?>> map = injector.getInstance(Key.getMapOf(new TypeLiteral<String>(){}, new TypeLiteral<Class<?>>(){}));

        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals(String.class, map.get("x"));

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals("map:3", service.getName());
    }

    @Test
    public void testMapInjection_Resumed() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_MapConfiguration.class);
            // bind 1
            binder.bindMap(String.class, Object.class,"xyz").put("x", "xvalue").put("y", "yvalue");
            // second binding attempt to the same map...
            binder.bindMap(String.class, Object.class,"xyz").put("z", "zvalue").put("x", "xvalue1");
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals(";x=xvalue1;y=yvalue;z=zvalue", service.getName());
    }

    @Test
    public void testMapInjection_OverrideExplicitlyBoundType() {
        Module m1 = binder -> {
            binder.bind(MockInterface5.class).to(MockImplementation5.class);
            binder.bind(MockInterface1.class).to(MockImplementation1_MapConfiguration.class);

            binder.bindMap(String.class, Object.class, "xyz").put("a", MockInterface5.class);
        };

        Module m2 = binder -> binder.bind(MockInterface5.class).toInstance(new MockInterface5() {

            @Override
            public String toString() {
                return "abc";
            }
        });

        MockInterface1 service = new DefaultInjector(m1, m2).getInstance(MockInterface1.class);
        assertEquals("Map element was not overridden in submodule", ";a=abc", service.getName());
    }

    @Test
    public void testMapInjection_OverrideImplicitlyBoundType() {
        Module m1 = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_MapConfiguration.class);
            binder.bindMap(String.class, Object.class, "xyz").put("a", MockImplementation5.class);
        };

        Module m2 = binder -> binder.bind(MockImplementation5.class).toInstance(new MockImplementation5() {

            @Override
            public String toString() {
                return "abc";
            }
        });

        MockInterface1 service = new DefaultInjector(m1, m2).getInstance(MockInterface1.class);
        assertEquals("Map element was not overridden in submodule", ";a=abc", service.getName());
    }

    @Test
    public void testListInjection_addValue() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfiguration.class);
            binder.bindList(Object.class, "xyz").add("xvalue").add("yvalue");
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals(";xvalue;yvalue", service.getName());
    }

    @Test
    public void testListInjection_addOrderedValues() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfiguration.class);
            binder.bind(MockInterface5.class).to(MockImplementation5.class);

            binder.bindList(Object.class, "xyz")
                    .add("1value")
                    .add("2value")
                    .addAfter("5value", MockInterface5.class)
                    .insertBefore("3value", MockInterface5.class)
                    .add(MockInterface5.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals(";1value;2value;3value;xyz;5value", service.getName());
    }

    @Test
    public void testListInjection_addAllValues() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfiguration.class);

            Collection<Object> firstList = new ArrayList<>();
            firstList.add("1value");
            firstList.add("2value");
            firstList.add("3value");

            Collection<Object> secondList = new ArrayList<>();
            secondList.add("6value");
            secondList.add("7value");
            secondList.add("8value");

            binder.bind(MockInterface5.class).to(MockImplementation5.class);

            binder.bindList(Object.class, "xyz")
                    .insertAllBefore(firstList, MockInterface5.class)
                    .addAllAfter(secondList, MockInterface5.class)
                    .add("5value")
                    .add(MockInterface5.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals(";1value;2value;3value;xyz;6value;7value;8value;5value", service.getName());
    }

    @Test
    public void testListInjection_addType() {
        Module module = binder -> {
            binder.bind(MockInterface5.class).to(MockImplementation5.class);
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfiguration.class);

            binder.bindList(Object.class, "xyz").add(MockInterface5.class).add("yvalue");
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals(";xyz;yvalue", service.getName());
    }

    @Test
    public void testListInjection_addOrderedTypes() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfiguration.class);
            binder.bind(MockInterface5.class).to(MockImplementation5.class);

            binder.bindList(Object.class, "xyz")
                    .add("1value")
                    .insertBefore("5value", MockInterface5.class)
                    .add("2value")
                    .addAfter("6value", MockInterface5.class)
                    .add("3value")
                    .add(MockInterface5.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals(";1value;2value;5value;xyz;6value;3value", service.getName());
    }

    @Test
    public void testListInjection_addTypeWithBinding() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfiguration.class);
            binder.bindList(Object.class, "xyz").add(MockImplementation5.class).add("yvalue");
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals(";xyz;yvalue", service.getName());
    }

    @Test
    public void testListInjection_empty() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfiguration.class);
            binder.bindList(Object.class,"xyz");
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals("", service.getName());
    }

    @Test
    public void testListInjection_resumed() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfiguration.class);

            binder.bindList(Object.class, "xyz").add("xvalue").add("yvalue");
            binder.bindList(Object.class, "xyz").add("avalue");
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals(";xvalue;yvalue;avalue", service.getName());
    }

    @Test
    public void testTypedListInjection() {
        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfigurationMock5.class);
            binder.bind(MockInterface2.class).to(MockImplementation2_ListConfiguration.class);

            // Bind list for MockImplementation2_ListConfiguration
            binder.bindList(Object.class,"xyz")
                    .add("xvalue")
                    .add("yvalue")
                    .add(MockImplementation5.class);

            // Bind list for MockImplementation1_ListConfigurationMock5
            binder.bindList(MockInterface5.class)
                    .add(MockImplementation5.class)
                    .add(new MockInterface5() {
                        @Override
                        public String toString() {
                            return "abc";
                        }
                    });

            binder.bindList(Object.class)
                    .add("avalue")
                    .add("bvalue")
                    .add(MockImplementation5.class);

            // Add to list for MockImplementation1_ListConfigurationMock5
            binder.bindList(MockInterface5.class)
                    .add(new MockInterface5() {
                        @Override
                        public String toString() {
                            return "cde";
                        }
                    });

            // Create named list for MockInterface5
            binder.bindList(MockInterface5.class, "another_binding")
                    .add(new MockInterface5() {
                        @Override
                        public String toString() {
                            return "fgh";
                        }
                    });
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals(";xyz;abc;cde", service.getName());

        MockInterface2 service2 = injector.getInstance(MockInterface2.class);
        assertNotNull(service2);
        assertTrue(service2 instanceof MockImplementation2_ListConfiguration);
        assertEquals(";xvalue;yvalue;xyz", service2.getName());
    }

    @Test
    public void testListInjection_OverrideExplicitlyBoundType() {
        Module m1 = binder -> {
            binder.bind(MockInterface5.class).to(MockImplementation5.class);
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfiguration.class);

            binder.bindList(Object.class, "xyz").add(MockInterface5.class);
        };

        Module m2 = binder -> binder.bind(MockInterface5.class).toInstance(new MockInterface5() {

            @Override
            public String toString() {
                return "abc";
            }
        });

        MockInterface1 service = new DefaultInjector(m1, m2).getInstance(MockInterface1.class);
        assertEquals("List element was not overridden in submodule", ";abc", service.getName());
    }

    @Test
    public void testListInjection_OverrideImplicitlyBoundType() {
        Module m1 = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_ListConfiguration.class);
            binder.bindList(Object.class, "xyz").add(MockImplementation5.class);
        };

        Module m2 = binder -> binder.bind(MockImplementation5.class).toInstance(new MockImplementation5() {

            @Override
            public String toString() {
                return "abc";
            }
        });

        MockInterface1 service = new DefaultInjector(m1, m2).getInstance(MockInterface1.class);
        assertEquals("List element was not overridden in submodule", ";abc", service.getName());
    }


    @Test
    public void testInjectorInjection() {
        Module module = binder -> binder.bind(MockInterface1.class).to(
                MockImplementation1_WithInjector.class);

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals("injector_not_null", service.getName());
    }

}
