package me.mykindos.betterpvp.core.framework.manager;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Manager<T> {

    @Getter
    protected final Map<String, T> objects = new ConcurrentHashMap<>();

    public void addObject(String identifier, T object){
        objects.put(identifier, object);
    }

    public void addObject(UUID identifier, T object){
        addObject(identifier.toString(), object);
    }

    public Optional<T> getObject(String identifier){
        return Optional.ofNullable(objects.get(identifier));
    }

    public Optional<T> getObject(UUID identifier){
        return getObject(identifier.toString());
    }

    public void removeObject(String identifier) {
        objects.remove(identifier);
    }

    public void loadFromList(List<T> objects) {
        objects.forEach(o -> addObject(String.valueOf(o.hashCode()), o));
    }

}
