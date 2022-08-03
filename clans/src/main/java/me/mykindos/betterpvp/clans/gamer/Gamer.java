package me.mykindos.betterpvp.clans.gamer;

import lombok.Data;
import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Data
public class Gamer implements Invitable {

    private final Client client;
    private final String uuid;

    private HashMap<String, RoleBuild> activeBuilds = new HashMap<>();
    private List<RoleBuild> builds = new ArrayList<>();
    private HashMap<Enum<?>, Object> properties = new HashMap<>();

    private int lastDamaged;

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(Enum<?> key) {
        return Optional.ofNullable((T) properties.getOrDefault(key, null));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(Enum<?> key, Class<T> type) {
        return Optional.ofNullable(type.cast(properties.getOrDefault(key, null)));
    }

    public void putProperty(Enum<?> key, Object object){
        properties.put(key, object);
    }

}
