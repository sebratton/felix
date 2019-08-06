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

import org.apache.felix.scr.component.manager.Parameters;
import org.apache.felix.scr.impl.inject.BindParameters;
import org.apache.felix.scr.impl.inject.ValueUtils;
import org.apache.felix.scr.impl.inject.ValueUtils.ValueType;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.RefPair;

public class ParametersImpl implements Parameters
{
    final BindParameters bp;

    public ParametersImpl(BindParameters bp)
    {
        this.bp = bp;
    }

    public Object[] getParameters(Class<?>... objTypes)
    {
        ComponentContextImpl<?> context = bp.getComponentContext();
        RefPair<?, ?> refPair = bp.getRefPair();
        Object[] objects = new Object[objTypes.length];
        for (int i = 0; i < objTypes.length; ++i)
        {
            //ValueType valType = ValueUtils.getValueType(objTypes[i]);
            objects[i] = ValueUtils.getValue(null, ValueType.ref_serviceType, null,
                context, refPair);
        }
        return objects;
    }
}
