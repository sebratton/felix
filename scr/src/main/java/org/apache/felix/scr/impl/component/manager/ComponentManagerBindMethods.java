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

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.impl.component.manager.ComponentManagerBindMethod.BindMethodType;
import org.apache.felix.scr.impl.inject.InitReferenceMethod;
import org.apache.felix.scr.impl.inject.ReferenceMethod;
import org.apache.felix.scr.impl.inject.ReferenceMethods;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

public class ComponentManagerBindMethods implements ReferenceMethods
{

    private final ReferenceMethod bind;
    private final ReferenceMethod updated;
    private final ReferenceMethod unbind;
    private final InitReferenceMethod init;

    ComponentManagerBindMethods(ComponentManager componentManager, ReferenceMetadata m_dependencyMetadata)
    {
        bind = new ComponentManagerBindMethod(componentManager, m_dependencyMetadata.getBind(),
            BindMethodType.BIND);
        updated = new ComponentManagerBindMethod(componentManager,
            m_dependencyMetadata.getUpdated(), BindMethodType.UPDATED);
        unbind = new ComponentManagerBindMethod(componentManager,
            m_dependencyMetadata.getUnbind(), BindMethodType.UNBIND);
        init = m_dependencyMetadata.getField() == null ? null
            : new ComponentManagerBindMethod(componentManager, m_dependencyMetadata.getField(),
                BindMethodType.INIT);
    }

    @Override
    public ReferenceMethod getBind()
    {
        return bind;
    }

    @Override
    public InitReferenceMethod getInit()
    {
        return init;
    }

    @Override
    public ReferenceMethod getUnbind()
    {
        return unbind;
    }

    @Override
    public ReferenceMethod getUpdated()
    {
        return updated;
    }
}
