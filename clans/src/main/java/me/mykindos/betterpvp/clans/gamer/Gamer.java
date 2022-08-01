package me.mykindos.betterpvp.clans.gamer;

import lombok.Data;
import me.mykindos.betterpvp.core.client.Client;

import java.util.HashMap;
import java.util.Optional;

@Data
public class Gamer {

    private final Client client;
    private final String uuid;

    private HashMap<String, Object> properties = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key) {
        return Optional.ofNullable((T) properties.getOrDefault(key, null));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        return Optional.ofNullable(type.cast(properties.getOrDefault(key, null)));
    }

    public void putProperty(String key, Object object){
        properties.put(key, object);
    }

}
