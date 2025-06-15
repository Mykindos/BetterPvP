package me.mykindos.betterpvp.core.item.component.impl.ability.serialization;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ItemAbilitySerializationRegistry {

    private final Map<Class<? extends ItemAbility>, ItemAbilitySerializer<?>> serializers = new HashMap<>();
    private final Map<NamespacedKey, ItemAbilityDeserializer<?>> deserializers = new HashMap<>();

    @Inject
    public ItemAbilitySerializationRegistry() {
        // No default serializers for abilities
    }

    /**
     * Register a serializer for an ability type.
     *
     * @param serializer The serializer to register
     * @param <T> The ability type
     */
    public <T extends ItemAbility> void registerSerializer(@NotNull Class<T> abilityType, @NotNull ItemAbilitySerializer<T> serializer) {
        serializers.put(abilityType, serializer);
    }

    /**
     * Register a deserializer for an ability type.
     *
     * @param deserializer The deserializer to register
     * @param <T> The ability type
     */
    public <T extends ItemAbility> void registerDeserializer(@NotNull ItemAbilityDeserializer<T> deserializer) {
        deserializers.put(deserializer.getKey(), deserializer);
    }

    /**
     * Get a serializer for an ability type.
     *
     * @param abilityType The ability type
     * @return Optional containing the serializer if found
     */
    @SuppressWarnings("unchecked")
    public <T extends ItemAbility> Optional<ItemAbilitySerializer<T>> getSerializer(@NotNull Class<T> abilityType) {
        return Optional.ofNullable((ItemAbilitySerializer<T>) serializers.get(abilityType));
    }

    /**
     * Get a deserializer for an ability key.
     *
     * @param key The ability key
     * @return Optional containing the deserializer if found
     */
    public Optional<ItemAbilityDeserializer<?>> getDeserializer(@NotNull NamespacedKey key) {
        return Optional.ofNullable(deserializers.get(key));
    }

    /**
     * Get all registered deserializers.
     *
     * @return Map of all deserializers
     */
    public Map<NamespacedKey, ItemAbilityDeserializer<?>> getAllDeserializers() {
        return Map.copyOf(deserializers);
    }

    /**
     * Check if an ability type can be serialized.
     *
     * @param abilityType The ability type to check
     * @return True if a serializer is registered for this type
     */
    public boolean canSerialize(@NotNull Class<? extends ItemAbility> abilityType) {
        return serializers.containsKey(abilityType);
    }

    /**
     * Check if an ability key can be deserialized.
     *
     * @param key The ability key to check
     * @return True if a deserializer is registered for this key
     */
    public boolean canDeserialize(@NotNull NamespacedKey key) {
        return deserializers.containsKey(key);
    }
} 