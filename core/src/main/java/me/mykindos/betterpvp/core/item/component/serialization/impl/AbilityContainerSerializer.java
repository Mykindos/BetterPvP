package me.mykindos.betterpvp.core.item.component.serialization.impl;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.serialization.ItemAbilitySerializationRegistry;
import me.mykindos.betterpvp.core.item.component.impl.ability.serialization.ItemAbilitySerializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializer and deserializer for AbilityContainerComponent.
 * This implementation stores ability keys and uses the ItemAbilityRegistry
 * to look up the actual ability instances.
 */
@CustomLog
public class AbilityContainerSerializer implements ComponentSerializer<AbilityContainerComponent>, ComponentDeserializer<AbilityContainerComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "abilities");
    private static final NamespacedKey LIST_KEY = new NamespacedKey("betterpvp", "list");
    
    private final ItemAbilitySerializationRegistry abilitySerializationRegistry;

    public AbilityContainerSerializer(ItemAbilitySerializationRegistry abilitySerializationRegistry) {
        this.abilitySerializationRegistry = abilitySerializationRegistry;
    }

    @Override
    @NotNull
    public Class<AbilityContainerComponent> getType() {
        return AbilityContainerComponent.class;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public void serialize(@NotNull AbilityContainerComponent instance, @NotNull PersistentDataContainer container) {
        List<String> abilities = new ArrayList<>();

        PersistentDataContainer abilitiesContainer = container.getAdapterContext().newPersistentDataContainer();
        for (ItemAbility ability : instance.getAbilities()) {
            Preconditions.checkNotNull(ability, "Ability cannot be null");
            NamespacedKey abilityKey = ability.getKey();
            Preconditions.checkNotNull(abilityKey, "Ability key cannot be null");

            // Try to serialize with a custom serializer if present
            var serializerOpt = abilitySerializationRegistry.getSerializer(ability.getClass());
            if (serializerOpt.isPresent()) {
                PersistentDataContainer abilityData = container.getAdapterContext().newPersistentDataContainer();
                //noinspection unchecked
                ((ItemAbilitySerializer<ItemAbility>) serializerOpt.get()).serialize(ability, abilityData);
                abilitiesContainer.set(abilityKey, PersistentDataType.TAG_CONTAINER, abilityData);
            }

            abilities.add(abilityKey.toString());
        }
        // Store the list of ability key strings for fallback/lookup
        abilitiesContainer.set(LIST_KEY, PersistentDataType.LIST.strings(), abilities);
        // Store the abilities' serialized data (if any)
        container.set(KEY, PersistentDataType.TAG_CONTAINER, abilitiesContainer);
    }

    @Override
    public void delete(@NotNull AbilityContainerComponent instance, @NotNull PersistentDataContainer container) {
        if (container.has(KEY, PersistentDataType.TAG_CONTAINER)) {
            container.remove(KEY);
        }
    }

    @Override
    public @NotNull AbilityContainerComponent deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        Preconditions.checkArgument(container.has(KEY, PersistentDataType.TAG_CONTAINER),
                "Container does not have abilities data");

        PersistentDataContainer abilitiesContainer = container.get(KEY, PersistentDataType.TAG_CONTAINER);
        Preconditions.checkNotNull(abilitiesContainer, "Abilities container is null");

        List<String> abilityKeyStrings = abilitiesContainer.get(LIST_KEY, PersistentDataType.LIST.strings());
        Preconditions.checkNotNull(abilityKeyStrings, "Ability key list is null");

        List<ItemAbility> abilities = new ArrayList<>();
        for (String keyString : abilityKeyStrings) {
            try {
                NamespacedKey abilityKey = NamespacedKey.fromString(keyString);
                if (abilityKey == null) {
                    log.error("Invalid ability key string: " + keyString).submit();
                    continue;
                }

                // Try to use a custom deserializer if present
                PersistentDataContainer abilityData = abilitiesContainer.get(abilityKey, PersistentDataType.TAG_CONTAINER);
                if (abilityData != null) {
                    var deserializerOpt = abilitySerializationRegistry.getDeserializer(abilityKey);
                    if (deserializerOpt.isPresent()) {
                        ItemAbility ability = deserializerOpt.get().deserialize(item, abilityData);
                        if (ability != null) {
                            abilities.add(ability);
                            continue;
                        } else {
                            log.error("Custom deserializer returned null for ability: " + abilityKey).submit();
                        }
                    } else {
                        log.error("Ability was serialized but no deserializer found for key: " + abilityKey).submit();
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Failed to parse ability key: " + keyString, e).submit();
            }
        }

        return AbilityContainerComponent.builder()
                .abilities(abilities)
                .build();
    }
} 