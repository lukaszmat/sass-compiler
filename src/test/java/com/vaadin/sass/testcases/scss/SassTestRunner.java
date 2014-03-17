/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.sass.testcases.scss;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * Test runner that executes methods annotated with @{@link FactoryTest} with
 * all the values returned by a method annotated with @{@link TestFactory} as
 * their parameters parameter.
 * 
 * This runner is loosely based on FactoryTestRunner by Ted Young
 * (http://tedyoung.me/2011/01/23/junit-runtime-tests-custom-runners/). The
 * generated test names give information about the parameters used (unlike
 * {@link Parameterized}).
 * 
 */
public class SassTestRunner extends BlockJUnit4ClassRunner {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestFactory {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface FactoryTest {
    }

    public SassTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> tests = new LinkedList<FrameworkMethod>();

        // Final all methods in our test class marked with @TestFactory.
        for (FrameworkMethod method : getTestClass().getAnnotatedMethods(
                TestFactory.class)) {
            // Make sure the TestFactory method is static
            if (!Modifier.isStatic(method.getMethod().getModifiers())) {
                throw new IllegalArgumentException("TestFactory " + method
                        + " must be static.");
            }

            // Execute the method (statically)
            Object params;
            try {
                params = method.getMethod().invoke(
                        getTestClass().getJavaClass());
            } catch (Throwable t) {
                throw new RuntimeException("Could not run test factory method "
                        + method.getName(), t);
            }

            // Did the factory return an array? If so, make it a list.
            if (params.getClass().isArray()) {
                params = Arrays.asList((Object[]) params);
            }

            // Did the factory return a scalar object? If so, put it in a list.
            if (!(params instanceof Iterable<?>)) {
                params = Collections.singletonList(params);
            }

            // For each object returned by the factory.
            for (Object param : (Iterable<?>) params) {
                // Find any methods marked with @SassTest.
                for (FrameworkMethod m : getTestClass().getAnnotatedMethods(
                        FactoryTest.class)) {
                    tests.add(new ParameterizedFrameworkMethod(m.getMethod(),
                            new Object[] { param }));
                }
            }
        }

        return tests;
    }

    private static class ParameterizedFrameworkMethod extends FrameworkMethod {
        private Object[] params;

        public ParameterizedFrameworkMethod(Method method, Object[] params) {
            super(method);
            this.params = params;
        }

        @Override
        public Object invokeExplosively(Object target, Object... params)
                throws Throwable {
            // Executes the test method with the supplied parameters (returned
            // by the
            // TestFactory) and not the instance generated by FrameworkMethod.
            return super.invokeExplosively(target, this.params);
        }

        @Override
        public String getName() {
            return String.format("%s[%s]", getMethod().getName(),
                    Arrays.toString(params));
        }
    }
}
