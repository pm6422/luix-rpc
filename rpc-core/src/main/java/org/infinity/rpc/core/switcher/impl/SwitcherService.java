package org.infinity.rpc.core.switcher.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.switcher.Switcher;
import org.infinity.rpc.core.switcher.SwitcherListener;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NotThreadSafe
public class SwitcherService {
    public static final String                              REGISTRY_HEARTBEAT_SWITCHER = "feature.configserver.heartbeat";
    private final       Map<String, Switcher>               switcherMap                 = new ConcurrentHashMap<>();
    private final       Map<String, List<SwitcherListener>> listenerMap                 = new ConcurrentHashMap<>();

    /**
     * Prohibit instantiate an instance outside the class
     */
    private SwitcherService() {
    }

    /**
     * Get the DefaultSwitcherService instance
     *
     * @return
     */
    public static SwitcherService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public Switcher getSwitcher(String name) {
        return switcherMap.get(name);
    }

    public List<Switcher> getAllSwitchers() {
        return new ArrayList<>(switcherMap.values());
    }

    public void initSwitcher(String name, boolean initialValue) {
        setValue(name, initialValue);
    }

    public void setValue(String name, boolean value) {
        putSwitcher(Switcher.of(name, value));
        publishChangeEvent(name, value);
    }

    private void putSwitcher(Switcher switcher) {
        switcherMap.put(switcher.getName(), switcher);
    }

    private void publishChangeEvent(String name, boolean value) {
        List<SwitcherListener> listeners = listenerMap.get(name);
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        // Invoke the change event
        listeners.forEach(listener -> listener.onSubscribe(name, value));
    }

    public boolean isOn(String name) {
        Switcher switcher = switcherMap.get(name);
        return switcher != null && switcher.isOn();
    }

    public boolean isOn(String name, boolean defaultValue) {
        Switcher switcher = switcherMap.get(name);
        if (switcher == null) {
            switcherMap.putIfAbsent(name, Switcher.of(name, defaultValue));
            switcher = switcherMap.get(name);
        }
        return switcher.isOn();
    }

    public void registerListener(String name, SwitcherListener listener) {
        List listeners = Collections.synchronizedList(new ArrayList());
        List existingListeners = listenerMap.putIfAbsent(name, listeners);
        if (existingListeners == null) {
            // Key does not exist in map, return null
            listeners.add(listener);
        } else {
            // Key exists in map, return old data
            existingListeners.add(listener);
        }
    }

    public void unregisterListener(String name, SwitcherListener listener) {
        List<SwitcherListener> listeners = listenerMap.get(name);
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.remove(listener);
        }
    }

    public void unregisterListeners(String name) {
        List<SwitcherListener> listeners = listenerMap.get(name);
        if (CollectionUtils.isNotEmpty(listeners)) {
            // clean all the listeners
            listeners.clear();
        }
    }

    /**
     * The singleton instance holder static inner class
     */
    private static class SingletonHolder {
        private static final SwitcherService INSTANCE = new SwitcherService();// static variable will be instantiated on class loading.
    }
}
