package me.mykindos.betterpvp.core.framework.manager;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public abstract class Manager<T> {

    @Getter
    protected final HashMap<String, T> objects = new HashMap<>();

    public void addObject(String identifier, T object){
        objects.put(identifier, object);
    }

    public Optional<T> getObject(String identifier){
        return Optional.ofNullable(objects.get(identifier));
    }

    public void removeObject(String identifier) {
        objects.remove(identifier);
    }

    public void loadFromList(List<T> objects) {
        objects.forEach(o -> addObject(String.valueOf(o.hashCode()), o));
    }

}
