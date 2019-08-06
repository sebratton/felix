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
import org.apache.felix.scr.component.manager.ReturnValue;
import org.apache.felix.scr.impl.inject.LifecycleMethod;
import org.apache.felix.scr.impl.inject.MethodResult;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.osgi.service.log.LogService;

public class ComponentManagerLifecycleMethod implements LifecycleMethod {

    public enum LifeCycleMethodType {
        ACTIVATE,
        DEACTIVATE,
        MODIFIED
    }
    
    private final ComponentManager m_componentLifecycleManager;

    private final LifeCycleMethodType methodType;

    public ComponentManagerLifecycleMethod(ComponentManager componentLifecycleManager, LifeCycleMethodType methodType)
    {
        m_componentLifecycleManager = componentLifecycleManager;
        this.methodType = methodType;
	}

	@Override
    public MethodResult invoke(Object componentInstance, ComponentContextImpl<?> componentContext, int reason,
            MethodResult methodCallFailureResult) {
        try {
            ReturnValue val;
            if (methodType == LifeCycleMethodType.ACTIVATE) {
                val = m_componentLifecycleManager.activate(componentInstance, componentContext);
            } else if (methodType == LifeCycleMethodType.DEACTIVATE) {
                val = m_componentLifecycleManager.deactivate(componentInstance, componentContext, reason);
            } else {
                val = m_componentLifecycleManager.modified(componentInstance, componentContext);
            }
            return val == ReturnValue.VOID ? MethodResult.VOID : new MethodResult(val.isVoid(), val.getReturnValue());
        } catch (Exception e) {
            componentContext.getLogger().log( LogService.LOG_ERROR, "The {0} method has thrown an exception", e,
                    methodType );
            if ( methodCallFailureResult != null && methodCallFailureResult.getResult() != null )
            {
                methodCallFailureResult.getResult().put("exception", e);
            }
        }
        return methodCallFailureResult;
	}

}