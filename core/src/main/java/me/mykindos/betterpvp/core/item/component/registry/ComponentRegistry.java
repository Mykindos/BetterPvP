package me.mykindos.betterpvp.core.item.component.registry;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of ComponentRegistry.
 * Manages all available components and provides utility methods for applying them to items.
 */
@CustomLog
@Singleton
public class ComponentRegistry {

    private final Set<ItemComponent> registeredComponents = new HashSet<>();

    public void registerComponent(@NotNull ItemComponent component) {
        registeredComponents.add(component);
    }

    @NotNull
    public Collection<ItemComponent> getAllComponents() {
        return Collections.unmodifiableCollection(registeredComponents);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends ItemComponent> Collection<T> getComponentsByType(@NotNull Class<T> type) {
        return registeredComponents.stream()
                .filter(type::isInstance)
                .map(component -> (T) component)
                .collect(Collectors.toList());
    }
} 