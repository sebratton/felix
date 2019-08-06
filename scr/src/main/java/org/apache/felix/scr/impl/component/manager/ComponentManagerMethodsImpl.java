/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl.component.manager;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.impl.component.manager.ComponentManagerLifecycleMethod.LifeCycleMethodType;
import org.apache.felix.scr.impl.inject.ComponentConstructor;
import org.apache.felix.scr.impl.inject.ComponentMethods;
import org.apache.felix.scr.impl.inject.LifecycleMethod;
import org.apache.felix.scr.impl.inject.ReferenceMethods;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

public class ComponentManagerMethodsImpl<T> implements ComponentMethods<T> {

    private final ComponentManager componentManager;

    private ComponentManagerLifecycleMethod activate;
    private ComponentManagerLifecycleMethod deactivate;
    private ComponentManagerLifecycleMethod modified;
    private ComponentConstructor<T> constructor;

    private final Map<String, ReferenceMethods> bindMethodMap = new HashMap<>();

    public ComponentManagerMethodsImpl(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    @Override
    public synchronized void initComponentMethods(ComponentMetadata componentMetadata, Class<T> implementationObjectClass,
            ComponentLogger logger) {
        if (activate != null) {
            return;
        }
        for ( ReferenceMetadata referenceMetadata: componentMetadata.getDependencies()) {
            final String refName = referenceMetadata.getName();
            if ( referenceMetadata.getField() != null || referenceMetadata.getBind() != null) {
                bindMethodMap.put(refName, new ComponentManagerBindMethods(componentManager, referenceMetadata));
            } else {
                bindMethodMap.put( refName, ReferenceMethods.NOPReferenceMethod );
            }
        }
        activate = new ComponentManagerLifecycleMethod(componentManager,
            LifeCycleMethodType.ACTIVATE);
        deactivate = new ComponentManagerLifecycleMethod(componentManager,
            LifeCycleMethodType.DEACTIVATE);
        modified = new ComponentManagerLifecycleMethod(componentManager,
            LifeCycleMethodType.MODIFIED);
        constructor = new ComponentConstructor<T>(componentMetadata,
            implementationObjectClass, logger);
    }

    @Override
    public LifecycleMethod getActivateMethod() {
        return activate;
    }

    @Override
    public LifecycleMethod getDeactivateMethod() {
        return deactivate;
    }

    @Override
    public LifecycleMethod getModifiedMethod() {
        return modified;
    }

    @Override
    public ReferenceMethods getBindMethods(String refName) {
        return bindMethodMap.get(refName);
    }

    @Override
    public ComponentConstructor<T> getConstructor() {
        return constructor;
    }
}