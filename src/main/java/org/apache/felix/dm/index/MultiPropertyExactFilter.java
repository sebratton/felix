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
package org.apache.felix.dm.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.tracker.ServiceTracker;
import org.apache.felix.dm.tracker.ServiceTrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class MultiPropertyExactFilter implements FilterIndex, ServiceTrackerCustomizer {
    private final Object m_lock = new Object();
    private ServiceTracker m_tracker;
    private BundleContext m_context;
    private final List /* <String> */ m_propertyKeys;
    private final Map /* <String, List<ServiceReference>> */ m_keyToServiceReferencesMap = new HashMap();
    private final Map /* <String, List<ServiceListener>> */ m_keyToListenersMap = new HashMap();
    private final Map /* <ServiceListener, String> */ m_listenerToFilterMap = new HashMap();

    public MultiPropertyExactFilter(String[] propertyKeys) {
        String[] keys = (String[]) Arrays.copyOf(propertyKeys, propertyKeys.length);
        Arrays.sort(keys);
        m_propertyKeys = Arrays.asList(keys);
    }
    
    public void open(BundleContext context) {
        synchronized (m_lock) {
            if (m_context != null) {
                throw new IllegalStateException("Filter already open.");
            }
            try {
                m_tracker = new ServiceTracker(context, context.createFilter("(" + Constants.OBJECTCLASS + "=*)"), this);
            }
            catch (InvalidSyntaxException e) {
                throw new Error();
            }
            m_context = context;
        }
        m_tracker.open(true);
    }

    public void close() {
        ServiceTracker tracker;
        synchronized (m_lock) {
            if (m_context == null) {
                throw new IllegalStateException("Filter already closed.");
            }
            tracker = m_tracker;
            m_tracker = null;
            m_context = null;
        }
        tracker.close();
    }

    public ServiceReference[] getAllServiceReferences(String clazz, String filter) {
        List /* <ServiceReference> */ result = new ArrayList();
        List keys = createKeysFromFilter(clazz, filter);
        Iterator iterator = keys.iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            ServiceReference reference;
            synchronized (m_keyToServiceReferencesMap) {
                List references = (List) m_keyToServiceReferencesMap.get(key);
                if (references != null) {
                    result.addAll(references);
                }
            }
        }
        if (result.size() == 0) {
            return null;
        }
        else {
            return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);
        }
    }

    public Object addingService(ServiceReference reference) {
        BundleContext context;
        synchronized (m_lock) {
            context = m_context;
        }
        if (context != null) {
            return context.getService(reference);
        }
        else {
            throw new IllegalStateException("No valid bundle context.");
        }
    }

    public void addedService(ServiceReference reference, Object service) {
        if (isApplicable(reference.getPropertyKeys())) {
            update(reference);
        }
    }

    public void modifiedService(ServiceReference reference, Object service) {
        if (isApplicable(reference.getPropertyKeys())) {
            update(reference);
        }
    }

    public void removedService(ServiceReference reference, Object service) {
        if (isApplicable(reference.getPropertyKeys())) {
            remove(reference);
        }
    }
    
    public void update(ServiceReference reference) {
        List /* <String> */ keys = createKeys(reference);
        synchronized (m_keyToServiceReferencesMap) {
            // TODO any 'old' references that are still in the index need to be removed in case of an update
            for (int i = 0; i < keys.size(); i++) {
                List /* <ServiceReference> */ references = (List) m_keyToServiceReferencesMap.get(keys.get(i));
                if (references == null) {
                    references = new ArrayList();
                    m_keyToServiceReferencesMap.put(keys.get(i), references);
                }
                references.add(reference);
            }
        }
    }

    public void remove(ServiceReference reference) {
        List /* <String> */ keys = createKeys(reference);
        synchronized (m_keyToServiceReferencesMap) {
            for (int i = 0; i < keys.size(); i++) {
                List /* <ServiceReference> */ references = (List) m_keyToServiceReferencesMap.get(keys.get(i));
                if (references != null) {
                    references.remove(reference);
                }
            }
        }
    }

    public boolean isApplicable(String[] propertyKeys) {
        List list = Arrays.asList(propertyKeys);
        Iterator iterator = m_propertyKeys.iterator();
        while (iterator.hasNext()) {
            String item = (String) iterator.next();
            if (!(list.contains(item))) {
                return false;
            }
        }
        return true;
    }
    
    public static void main(String[] args) {
        
        System.out.println("" + (new MultiPropertyExactFilter(new String[] { "objectClass", "repository", "path", "name" })).isApplicable(null, "(objectClass=abc)"));
    }
    
    public boolean isApplicable(String clazz, String filter) {
        // "(&(a=b)(c=d))"
        // "(&(&(a=b)(c=d))(objC=aaa))"
        // startsWith "(&" en split in "(x=y)" -> elke x bestaat in m_propertykeys
        
        // (&(objectClass=xyz)(&(a=x)(b=y)))
        
        Set /* <String> */found = new HashSet();
        if (filter != null && filter.startsWith("(&(objectClass=") && filter.contains(")(&(") && filter.endsWith(")))")) {
            int i1 = filter.indexOf(")(&(");
            String className = filter.substring("(&(objectClass=".length(), i1);
            if (!m_propertyKeys.contains("objectClass")) {
                return false;
            }
            else {
                found.add("objectClass");
            }
            String[] parts = filter.substring(i1 + ")(&(".length(), filter.length() - ")))".length()).split("\\)\\(");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                String[] tuple = part.split("=");
                if (!m_propertyKeys.contains(tuple[0])) {
                    return false;
                }
                else {
                    found.add(tuple[0]);
                }
                // TODO check value tuple[1]
            }
            return (found.size() == m_propertyKeys.size());
        }
        else if (filter != null && filter.startsWith("(&(") && filter.endsWith("))")) {
            String[] parts = filter.substring(3, filter.length() - 2).split("\\)\\(");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                String[] tuple = part.split("=");
                if (!m_propertyKeys.contains(tuple[0])) {
                    return false;
                }
                else {
                    found.add(tuple[0]);
                }
                // TODO check value tuple[1]
            }
            return (found.size() == m_propertyKeys.size());
        }
        else if (filter != null && filter.startsWith("(") && filter.endsWith(")") && m_propertyKeys.size() == 1) { // TODO quick hack
            String part = filter.substring(1, filter.length() - 1);
            String[] tuple = part.split("=");
            if (!m_propertyKeys.contains(tuple[0])) {
                return false;
            }
            else {
                return true;
            }
        }
        else if (clazz != null && filter == null && m_propertyKeys.size() == 1 && m_propertyKeys.get(0).equals(Constants.OBJECTCLASS)) {
            return true;
        }
        return false;
    }
    
    private List /* <String> */ createKeys(ServiceReference reference) {
        List /* <String> */ results = new ArrayList();
        
        results.add("");
        
        String[] keys = reference.getPropertyKeys();
        Arrays.sort(keys);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (m_propertyKeys.contains(key)) {
                Object value = reference.getProperty(key);
                if (value instanceof String[]) {
                    String[] values = (String[]) value;
                    List newResults = new ArrayList();
                    for (int j = 0; j < values.length; j++) {
                        String val = values[j];
                        for (int k = 0; k < results.size(); k++) {
                            String head = (String) results.get(k);
                            if (head != null && head.length() > 0) {
                                head = head + ";";
                            }
                            newResults.add(head + key + "=" + val);
                        }
                    }
                    results = newResults;
                }
                else {
                    for (int k = 0; k < results.size(); k++) {
                        String head = (String) results.get(k);
                        if (head != null && head.length() > 0) {
                            head = head + ";";
                        }
                        results.set(k, head + key + "=" + value);
                    }
                }
            }
        }
        return results;
    }
    
    private List /* <String> */ createKeysFromFilter(String clazz, String filter) {
        // TODO array support
        List result = new ArrayList();
        StringBuffer index = new StringBuffer();
        Iterator iterator = m_propertyKeys.iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            if (index.length() > 0) {
                index.append(';');
            }
            index.append(key);
            index.append('=');
            String value = null;
            if (clazz != null && Constants.OBJECTCLASS.equals(key)) {
                value = clazz;
            } // (&(obC=a)(&(a=b)(c=d)))
            if (filter != null) {
                String startString = "(" + key + "=";
                int i1 = filter.indexOf(startString);
                if (i1 != -1) {
                    int i2 = filter.indexOf(")(", i1);
                    if (i2 == -1) {
                        if (filter.endsWith(")))")) {
                            i2 = filter.length() - 3;
                        }
                        else if (filter.endsWith("))")) {
                            i2 = filter.length() - 2;
                        }
                        else {
                            i2 = filter.length() - 1;
                        }
                    }
                    String value2 = filter.substring(i1 + startString.length(), i2);
                    if (value != null && !value.equals(value2)) {
                        // corner case: someone specified a clazz and
                        // also a filter containing a different clazz
                        return result;
                    }
                    value = value2;
                }
            }
            index.append(value);
        }
        result.add(index.toString());
        return result;
    }

    public void serviceChanged(ServiceEvent event) {
        if (isApplicable(event.getServiceReference().getPropertyKeys())) {
            List /* <String> */ keys = createKeys(event.getServiceReference());
            List list = new ArrayList();
            synchronized (m_keyToListenersMap) {
                for (int i = 0; i < keys.size(); i++) {
                    String key = (String) keys.get(i);
                    List listeners = (List) m_keyToListenersMap.get(key);
                    if (listeners != null) {
                        list.addAll(listeners);
                    }
                }
            }
            if (list != null) {
                Iterator iterator = list.iterator();
                while (iterator.hasNext()) {
                    ServiceListener listener = (ServiceListener) iterator.next();
                    listener.serviceChanged(event);
                }
            }
        }
    }

    public void addServiceListener(ServiceListener listener, String filter) {
        List keys = createKeysFromFilter(null, filter);
        Iterator iterator = keys.iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            synchronized (m_keyToListenersMap) {
                List /* <ServiceListener> */ listeners = (List) m_keyToListenersMap.get(key);
                if (listeners == null) {
                    listeners = new CopyOnWriteArrayList();
                    m_keyToListenersMap.put(key, listeners);
                }
                listeners.add(listener);
                m_listenerToFilterMap.put(listener, filter);
            }
        }
    }

    public void removeServiceListener(ServiceListener listener) {
        synchronized (m_keyToListenersMap) {
            String filter = (String) m_listenerToFilterMap.remove(listener);
            List keys = createKeysFromFilter(null, filter);
            Iterator iterator = keys.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                
                boolean result = filter != null;
                if (result) {
                    List /* <ServiceListener> */ listeners = (List) m_keyToListenersMap.get(key);
                    if (listeners != null) {
                        listeners.remove(listener);
                    }
                    // TODO actually, if listeners == null that would be strange....
                }
            }
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("MultiPropertyExactFilter[");
        sb.append("K2L: " + m_keyToListenersMap.size());
        sb.append(", K2SR: " + m_keyToServiceReferencesMap.size());
        sb.append(", L2F: " + m_listenerToFilterMap.size());
        sb.append("]");
        return sb.toString();
    }
}
