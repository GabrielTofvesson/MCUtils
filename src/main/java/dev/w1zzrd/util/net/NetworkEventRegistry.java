package dev.w1zzrd.util.net;

import dev.w1zzrd.util.support.Pair;
import java.util.ArrayList;

public final class NetworkEventRegistry {
    public static final NetworkEventRegistry INSTANCE = new NetworkEventRegistry();

    /**
     * Set of all event types
     */
    private final ArrayList<Pair<Class<? extends ContextEvent>, ContextEventFactory>> eventTypes = new ArrayList<>();

    private NetworkEventRegistry(){ }


    public <T extends ContextEvent> boolean registerEventType(Class<T> eventType)
    {
        return registerEventType(eventType, () -> eventType.getDeclaredConstructor().newInstance());
    }

    /**
     * Register an event type with the network event registry
     * @param eventType Event class to register
     * @return True if event was registered. False if it is already registered
     */
    public boolean registerEventType(Class<? extends ContextEvent> eventType, ContextEventFactory factory)
    {
        // If any registered event type matches the argument, just return false
        if(eventTypes.stream().map(Pair::getKey).anyMatch(eventType::equals))
            return false;

        eventTypes.add(new Pair<>(eventType, factory));
        return true;
    }

    /**
     * Get numerical id of a given event type
     * @param eventType Event type to get id of
     * @return Id of event if it is registered, else -1
     */
    public int getEventId(Class<? extends ContextEvent> eventType)
    {
        for (int i = eventTypes.size() - 1; i >= 0; --i)
            if (eventTypes.get(i).getKey() == eventType)
                return i;

        return -1;
    }

    /**
     * Attempt to get an event type from the registry.
     * @param id ID of the event type to fetch
     * @return Null if ID didn't match an event type, else the event type
     */
    public Class<? extends ContextEvent> getEventType(int id)
    {
        if (id >= eventTypes.size() || id < 0)
            return null;

        return eventTypes.get(id).getKey();
    }

    /**
     * Attempt to create an instance of a given event type.
     * @param id Event type id
     * @param <T> Type of the event
     * @return An instance of the event type, if ID matches a valid type and said type declares an empty-arg constructor
     */
    public <T extends ContextEvent> T makeEvent(int id)
    {
        if (id >= eventTypes.size() || id < 0)
            return null;

        try {
            return (T) eventTypes.get(id).getValue().createContextEvent();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return null;
    }

    /**
     * Check if a given event type is registered
     */
    public boolean isRegistered(Class<? extends ContextEvent> eventType)
    {
        return getEventId(eventType) != -1;
    }
}
