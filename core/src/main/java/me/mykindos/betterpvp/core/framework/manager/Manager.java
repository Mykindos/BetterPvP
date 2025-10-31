package me.mykindos.betterpvp.core.framework.manager;

import lombok.CustomLog;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CustomLog
public abstract class Manager<T, R> {

    @Getter
    protected final Map<T, R> objects = new ConcurrentHashMap<>();

    public void addObject(T identifier, R object){
        objects.put(identifier, object);
    }

    public void addObject(UUID identifier, R object){
        try {
            objects.put((T) identifier.toString(), object);
        } catch (Exception e) {
            log.error("Failed to convert UUID to correct manager type, you should fix this", e).submit();
        }
    }

    public Optional<R> getObject(T identifier){
        return Optional.ofNullable(objects.get(identifier));
    }

    public Optional<R> getObject(UUID identifier){
        return Optional.ofNullable(objects.get(identifier.toString()));
    }

    public void removeObject(T identifier) {
        objects.remove(identifier);
    }

}
