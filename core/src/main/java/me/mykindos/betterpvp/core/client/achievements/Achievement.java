package me.mykindos.betterpvp.core.client.achievements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * todo
 */
@CustomLog
public abstract class Achievement<T extends PropertyContainer, E extends PropertyUpdateEvent<T>> implements IAchievement<T, E>, Listener {

    @Getter
    private final String name;
    @Getter
    private final Set<String> watchedProperties = new HashSet<>();

    public Achievement(String name, String... watchedProperties) {
        this.name = name;
        this.watchedProperties.addAll(Arrays.stream(watchedProperties).toList());
    }

    public Achievement(String name, Enum<?>... watchedProperties) {
        this.name = name;
        this.watchedProperties.addAll(Arrays.stream(watchedProperties)
                .map(Enum::name)
                .toList()
        );
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @Override
    public void onPropertyChangeListener(E event) {
        log.info(event.toString()).submit();
        final String changedProperty = event.getProperty();
        final Object value = event.getValue();
        final T container = event.getContainer();
        if (!watchedProperties.contains(changedProperty)) return;

        Map<String, Object> otherProperties = new HashMap<>();
        watchedProperties.stream()
                .filter(property -> !property.equals(changedProperty))
                .forEach(property -> {
                    otherProperties.put(property, container.getProperty(property).orElseThrow());
                });

        onChangeValue(container, changedProperty, value, otherProperties);

    }
}
