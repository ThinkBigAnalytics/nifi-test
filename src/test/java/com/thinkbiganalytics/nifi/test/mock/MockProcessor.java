/*
 * Copyright (c) 2018 ThinkBig Analytics, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 
package com.thinkbiganalytics.nifi.test.mock;


import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class MockProcessor implements Processor {

    private final Processor delegate;
    private ComponentLog logger;

    protected MockProcessor(String delegateClassName) {
        try {

            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            final Class<?> delegateClass = Class.forName(delegateClassName, true, contextClassLoader);

            delegate = (Processor) delegateClass.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }


    }

    protected Processor getDelegate() {
        return delegate;

    }

    protected final ComponentLog getLogger() {
        return logger;
    }

    @Override
    public void initialize(ProcessorInitializationContext processorInitializationContext) {
        getDelegate().initialize(processorInitializationContext);
        logger = processorInitializationContext.getLogger();
    }

    @Override
    public Set<Relationship> getRelationships() {
        return getDelegate().getRelationships();
    }

    @Override
    public abstract void onTrigger(ProcessContext processContext, ProcessSessionFactory processSessionFactory);

    @Override
    public Collection<ValidationResult> validate(ValidationContext validationContext) {
        return getDelegate().validate(validationContext);
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String s) {
        return getDelegate().getPropertyDescriptor(s);
    }

    @Override
    public void onPropertyModified(PropertyDescriptor propertyDescriptor, String s, String s1) {
        getDelegate().onPropertyModified(propertyDescriptor, s, s1);
    }

    @Override
    public List<PropertyDescriptor> getPropertyDescriptors() {
        return getDelegate().getPropertyDescriptors();
    }

    @Override
    public String getIdentifier() {
        return getDelegate().getIdentifier();
    }
}
