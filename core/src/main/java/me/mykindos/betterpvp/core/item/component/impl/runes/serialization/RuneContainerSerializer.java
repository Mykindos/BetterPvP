package me.mykindos.betterpvp.core.item.component.impl.runes.serialization;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneRegistry;
import me.mykindos.betterpvp.core.item.ItemInstance;
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
public class RuneContainerSerializer implements ComponentSerializer<RuneContainerComponent>, ComponentDeserializer<RuneContainerComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "runes");
    private static final NamespacedKey MAX_SLOTS_KEY = new NamespacedKey("betterpvp", "rune-max-slots");
    private static final NamespacedKey LIST_KEY = new NamespacedKey("betterpvp", "rune-list");

    private final RuneRegistry runeRegistry;
    
    @Inject
    public RuneContainerSerializer(RuneRegistry runeRegistry) {
        this.runeRegistry = runeRegistry;
    }
    
    @Override
    public @NotNull Class<RuneContainerComponent> getType() {
        return RuneContainerComponent.class;
    }
    
    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }
    
    @Override
    public void serialize(@NotNull RuneContainerComponent instance, @NotNull PersistentDataContainer container) {
        // Create runes container if it doesn't exist
        if (!container.has(KEY, PersistentDataType.TAG_CONTAINER)) {
            container.set(KEY, PersistentDataType.TAG_CONTAINER, container.getAdapterContext().newPersistentDataContainer());
        }

        PersistentDataContainer runesContainer = Objects.requireNonNull(container.get(KEY, PersistentDataType.TAG_CONTAINER));
        runesContainer.set(MAX_SLOTS_KEY, PersistentDataType.INTEGER, instance.getSlots());

        List<String> runeKeys = instance.getRunes().stream()
                .map(rune -> rune.getKey().toString())
                .toList();
        runesContainer.set(LIST_KEY, PersistentDataType.LIST.strings(), runeKeys);
        container.set(KEY, PersistentDataType.TAG_CONTAINER, runesContainer);
    }
    
    @Override
    public void delete(@NotNull RuneContainerComponent instance, @NotNull PersistentDataContainer container) {
        if (container.has(KEY, PersistentDataType.TAG_CONTAINER)) {
            container.remove(KEY);
        }
    }
    
    @Override
    public @NotNull RuneContainerComponent deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        Preconditions.checkNotNull(item, "ItemInstance cannot be null");
        Preconditions.checkArgument(container.has(KEY, PersistentDataType.TAG_CONTAINER), "PersistentDataContainer does not contain the RuneContainerComponent key");

        PersistentDataContainer runesContainer = Objects.requireNonNull(container.get(KEY, PersistentDataType.TAG_CONTAINER));
        Integer maxSlots = runesContainer.get(MAX_SLOTS_KEY, PersistentDataType.INTEGER);
        Preconditions.checkArgument(maxSlots != null, "RuneContainerComponent max slots cannot be null");

        List<Rune> runes = new ArrayList<>();
        List<String> runeKeys = runesContainer.get(LIST_KEY, PersistentDataType.LIST.strings());
        Preconditions.checkArgument(runeKeys != null, "RuneContainerComponent rune list cannot be null");
        for (String runeKey : runeKeys) {
            final NamespacedKey key = Objects.requireNonNull(NamespacedKey.fromString(runeKey));
            Optional<Rune> runeOpt = runeRegistry.getRune(key);
            if (runeOpt.isPresent()) {
                runes.add(runeOpt.get());
            } else {
                log.warn("Rune with key {} not found in registry, skipping.", runeKey);
            }
        }
        return new RuneContainerComponent(maxSlots, runes);
    }
} 