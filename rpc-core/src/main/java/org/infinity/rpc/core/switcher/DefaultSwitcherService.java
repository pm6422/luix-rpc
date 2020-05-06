package org.infinity.rpc.core.switcher;

import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServiceName(DefaultSwitcherService.SERVICE_NAME)
@ThreadSafe
public class DefaultSwitcherService implements SwitcherService {
    public static final String                              SERVICE_NAME = "defaultSwitcherService";
    private final       Map<String, Switcher>               switcherMap  = new ConcurrentHashMap<>();
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
        return switcherMap.get(name);
    }

    @Override
    public List<Switcher> getAllSwitchers() {
        return new ArrayList<>(switcherMap.values());
    }

    @Override
    public void initSwitcher(String name, boolean initialValue) {
        setValue(name, initialValue);
    }

    @Override
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
        // trigger the change event
        listeners.forEach(listener -> listener.onSubscribe(name, value));
    }

    @Override
    public boolean isOn(String name) {
        Switcher switcher = switcherMap.get(name);
        return switcher != null && switcher.isOn();
    }

    @Override
    public boolean isOn(String name, boolean defaultValue) {
        Switcher switcher = switcherMap.get(name);
        if (switcher == null) {
            switcherMap.putIfAbsent(name, Switcher.of(name, defaultValue));
            switcher = switcherMap.get(name);
        }
        return switcher.isOn();
    }

    @Override
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

    @Override
    public void unregisterListener(String name, SwitcherListener listener) {
        List<SwitcherListener> listeners = listenerMap.get(name);
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.remove(listener);
        }
    }

    @Override
    public void unregisterListeners(String name) {
        List<SwitcherListener> listeners = listenerMap.get(name);
        if (CollectionUtils.isNotEmpty(listeners)) {
            // clean all the listeners
            listeners.clear();
        }
    }
}
