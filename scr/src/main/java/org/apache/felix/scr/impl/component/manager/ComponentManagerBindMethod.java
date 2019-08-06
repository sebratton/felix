/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.felix.scr.impl.component.manager;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ReturnValue;
import org.apache.felix.scr.impl.inject.BindParameters;
import org.apache.felix.scr.impl.inject.InitReferenceMethod;
import org.apache.felix.scr.impl.inject.MethodResult;
import org.apache.felix.scr.impl.inject.ReferenceMethod;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

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
public class ComponentManagerBindMethod implements ReferenceMethod, InitReferenceMethod {

    public enum BindMethodType {
        BIND,
        UNBIND,
        UPDATED,
        INIT
    }
    
    private final ComponentManager componentManager;
    private final String name;
    private final BindMethodType methodType;

    public ComponentManagerBindMethod(ComponentManager componentManager, String name, BindMethodType methodType)
    {
        this.componentManager = componentManager;
        this.name = name;
        this.methodType = methodType;
	}

    @Override
    public <S, T> boolean getServiceObject(BindParameters parameters, BundleContext context) {
        if ( parameters.getServiceObject() == null) {
            return parameters.getServiceObject(context);
        }
        return true;
    }

    @Override
    public <S, T> MethodResult invoke(Object componentInstance, BindParameters parameter, MethodResult methodCallFailureResult) {
        try {
            ReturnValue val;
            if (methodType == BindMethodType.BIND) {
                val = componentManager.bind(componentInstance, name,
                    new ParametersImpl(parameter));
            } else if (methodType == BindMethodType.UNBIND) {
                val = componentManager.unbind(componentInstance, name,
                    new ParametersImpl(parameter));
            } else {
                val = componentManager.updated(componentInstance, name,
                    new ParametersImpl(parameter));
            }
            return val == ReturnValue.VOID ? MethodResult.VOID : new MethodResult(val.isVoid(), val.getReturnValue());
        } catch (Exception e) {
            parameter.getComponentContext().getLogger().log( LogService.LOG_ERROR, "The {0} method has thrown an exception", e,
                    methodType );
            if ( methodCallFailureResult != null && methodCallFailureResult.getResult() != null )
            {
                methodCallFailureResult.getResult().put("exception", e);
            }
        }
        return methodCallFailureResult;
    }

    @Override
    public boolean init(Object instance, ComponentLogger logger) {
        return componentManager.init(instance, name);
    }

}