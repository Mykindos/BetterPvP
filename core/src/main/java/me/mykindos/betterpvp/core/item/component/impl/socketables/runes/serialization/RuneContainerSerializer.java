package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.serialization;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableRegistry;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles serialization and deserialization of RuneContainerComponent.
 * Integrates with the BetterPvP configuration system for defaults.
 */
@CustomLog
public class RuneContainerSerializer implements ComponentSerializer<SocketableContainerComponent>, ComponentDeserializer<SocketableContainerComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "runes");
    private static final NamespacedKey SOCKETS_KEY = new NamespacedKey("betterpvp", "rune-sockets");
    private static final NamespacedKey MAX_SOCKETS_KEY = new NamespacedKey("betterpvp", "rune-max-sockets");
    private static final NamespacedKey LIST_KEY = new NamespacedKey("betterpvp", "rune-list");

    private final SocketableRegistry socketableRegistry;
    
    @Inject
    public RuneContainerSerializer(SocketableRegistry socketableRegistry) {
        this.socketableRegistry = socketableRegistry;
    }
    
    @Override
    public @NotNull Class<SocketableContainerComponent> getType() {
        return SocketableContainerComponent.class;
    }
    
    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }
    
    @Override
    public void serialize(@NotNull SocketableContainerComponent instance, @NotNull PersistentDataContainer container) {
        // Create runes container if it doesn't exist
        if (!container.has(KEY, PersistentDataType.TAG_CONTAINER)) {
            container.set(KEY, PersistentDataType.TAG_CONTAINER, container.getAdapterContext().newPersistentDataContainer());
        }

        PersistentDataContainer runesContainer = Objects.requireNonNull(container.get(KEY, PersistentDataType.TAG_CONTAINER));
        runesContainer.set(SOCKETS_KEY, PersistentDataType.INTEGER, instance.getSockets());
        runesContainer.set(MAX_SOCKETS_KEY, PersistentDataType.INTEGER, instance.getMaxSockets());

        List<String> runeKeys = instance.getSocketables().stream()
                .map(rune -> rune.getKey().toString())
                .toList();
        runesContainer.set(LIST_KEY, PersistentDataType.LIST.strings(), runeKeys);
        container.set(KEY, PersistentDataType.TAG_CONTAINER, runesContainer);
    }
    
    @Override
    public void delete(@NotNull SocketableContainerComponent instance, @NotNull PersistentDataContainer container) {
        if (container.has(KEY, PersistentDataType.TAG_CONTAINER)) {
            container.remove(KEY);
        }
    }
    
    @Override
    public @NotNull SocketableContainerComponent deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        Preconditions.checkNotNull(item, "ItemInstance cannot be null");
        Preconditions.checkArgument(container.has(KEY, PersistentDataType.TAG_CONTAINER), "PersistentDataContainer does not contain the RuneContainerComponent key");

        PersistentDataContainer runesContainer = Objects.requireNonNull(container.get(KEY, PersistentDataType.TAG_CONTAINER));
        Integer sockets = runesContainer.get(SOCKETS_KEY, PersistentDataType.INTEGER);
        Preconditions.checkArgument(sockets != null, "RuneContainerComponent slots cannot be null");

        // Backwards compatibility: old items don't have maxSockets saved
        Integer maxSockets = runesContainer.get(MAX_SOCKETS_KEY, PersistentDataType.INTEGER);
        Preconditions.checkArgument(maxSockets != null, "RuneContainerComponent max slots cannot be null");

        List<Socketable> socketables = new ArrayList<>();
        List<String> runeKeys = runesContainer.get(LIST_KEY, PersistentDataType.LIST.strings());
        Preconditions.checkArgument(runeKeys != null, "RuneContainerComponent rune list cannot be null");
        for (String runeKey : runeKeys) {
            final NamespacedKey key = Objects.requireNonNull(NamespacedKey.fromString(runeKey));
            Optional<Socketable> runeOpt = socketableRegistry.getRune(key);
            if (runeOpt.isPresent()) {
                socketables.add(runeOpt.get());
            } else {
                log.warn("Rune with key {} not found in registry, skipping.", runeKey).submit();
            }
        }
        return new SocketableContainerComponent(sockets, maxSockets, socketables);
    }
} 