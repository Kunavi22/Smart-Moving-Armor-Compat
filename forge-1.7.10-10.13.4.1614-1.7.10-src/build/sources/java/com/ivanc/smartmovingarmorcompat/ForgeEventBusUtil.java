package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.IEventListener;
import cpw.mods.fml.common.eventhandler.ListenerList;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.common.MinecraftForge;

final class ForgeEventBusUtil {
    private ForgeEventBusUtil() {
    }

    static boolean unregisterHandlersByClassName(String className) {
        try {
            Field listenersField = EventBus.class.getDeclaredField("listeners");
            listenersField.setAccessible(true);
            ConcurrentHashMap<?, ?> listeners = (ConcurrentHashMap<?, ?>) listenersField.get(MinecraftForge.EVENT_BUS);
            Set<?> keys = listeners.keySet();
            ArrayList<Object> removals = new ArrayList<Object>();

            for (Object key : keys) {
                if (key != null && className.equals(key.getClass().getName())) {
                    removals.add(key);
                }
            }

            for (int i = 0; i < removals.size(); i++) {
                MinecraftForge.EVENT_BUS.unregister(removals.get(i));
            }

            return !removals.isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean unregisterHandlerMethodByClassName(String className, String methodName) {
        try {
            EventBus bus = MinecraftForge.EVENT_BUS;
            Field listenersField = EventBus.class.getDeclaredField("listeners");
            listenersField.setAccessible(true);
            ConcurrentHashMap<?, ?> listeners = (ConcurrentHashMap<?, ?>) listenersField.get(bus);

            Field busIdField = EventBus.class.getDeclaredField("busID");
            busIdField.setAccessible(true);
            int busID = busIdField.getInt(bus);

            boolean removed = false;
            Set<?> keys = listeners.keySet();
            for (Object key : keys) {
                if (key == null || !className.equals(key.getClass().getName())) {
                    continue;
                }

                Object value = listeners.get(key);
                if (!(value instanceof List)) {
                    continue;
                }

                List<?> handlerList = (List<?>) value;
                Iterator<?> iterator = handlerList.iterator();
                while (iterator.hasNext()) {
                    Object listener = iterator.next();
                    if (!(listener instanceof IEventListener) || !isListenerMethod(listener, methodName)) {
                        continue;
                    }

                    ListenerList.unregisterAll(busID, (IEventListener) listener);
                    iterator.remove();
                    removed = true;
                }
            }

            return removed;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isListenerMethod(Object listener, String methodName) {
        String readable = String.valueOf(listener);
        return readable.indexOf(" " + methodName + "(") >= 0 || readable.indexOf(methodName + "(") >= 0;
    }
}
