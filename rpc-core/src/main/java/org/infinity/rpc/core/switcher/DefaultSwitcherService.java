/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.infinity.rpc.core.switcher;

import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServiceName(DefaultSwitcherService.SERVICE_NAME)
public class DefaultSwitcherService implements SwitcherService {
    public static final String                              SERVICE_NAME = "defaultSwitcherService";
    private final       Map<String, Switcher>               switchers    = new ConcurrentHashMap<>();
    private final       Map<String, List<SwitcherListener>> listenerMap  = new ConcurrentHashMap<>();

    /**
     * Get the DefaultSwitcherService instance
     *
     * @return
     */
    public static SwitcherService getInstance() {
        return ServiceInstanceLoader.getServiceLoader(SwitcherService.class).load(DefaultSwitcherService.SERVICE_NAME);
    }

    @Override
    public Switcher getSwitcher(String name) {
        return switchers.get(name);
    }

    @Override
    public List<Switcher> getAllSwitchers() {
        return new ArrayList<Switcher>(switchers.values());
    }

    @Override
    public void initSwitcher(String switcherName, boolean initialValue) {
        setValue(switcherName, initialValue);
    }

    @Override
    public boolean isOpen(String switcherName) {
        Switcher switcher = switchers.get(switcherName);
        return switcher != null && switcher.isOn();
    }

    @Override
    public boolean isOpen(String switcherName, boolean defaultValue) {
        Switcher switcher = switchers.get(switcherName);
        if (switcher == null) {
            switchers.putIfAbsent(switcherName, new Switcher(switcherName, defaultValue));
            switcher = switchers.get(switcherName);
        }
        return switcher.isOn();
    }

    @Override
    public void setValue(String switcherName, boolean value) {
        putSwitcher(new Switcher(switcherName, value));
        triggerChangeEvent(switcherName, value);
    }

    private void putSwitcher(Switcher switcher) {
        if (switcher == null) {
            throw new RuntimeException("LocalSwitcherService addSwitcher Error: switcher is null");
        }
        switchers.put(switcher.getName(), switcher);
    }

    private void triggerChangeEvent(String switcherName, boolean value) {
        List<SwitcherListener> listeners = listenerMap.get(switcherName);
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.forEach(listener -> listener.onSubscribe(switcherName, value));
        }
    }

    @Override
    public void registerListener(String switcherName, SwitcherListener listener) {
        List listeners = Collections.synchronizedList(new ArrayList());
        List preListeners = listenerMap.putIfAbsent(switcherName, listeners);
        if (preListeners == null) {
            listeners.add(listener);
        } else {
            preListeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(String switcherName, SwitcherListener listener) {
        List<SwitcherListener> listeners = listenerMap.get(switcherName);
        if (listener == null) {
            // keep empty listeners
            listeners.clear();
        } else {
            listeners.remove(listener);
        }
    }
}
