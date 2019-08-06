/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.component.manager;

/**
 * Bundles which to avoid reflective management of their
 * SCR components implement and register an implementation of this
 * interface via a bundle header. Implementations of this are expected
 * to be able to construct and inject fields for all components in their
 * bundle. 
 */
public interface ComponentManagerFactory {
    public ComponentManager createComponentManager(String componentName);
}