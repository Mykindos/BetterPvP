package me.mykindos.betterpvp.core.item.component.serialization;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.impl.fuel.FuelComponentSerializer;
import me.mykindos.betterpvp.core.item.component.impl.ability.serialization.ItemAbilitySerializationRegistry;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.StatSerializationRegistry;
import me.mykindos.betterpvp.core.item.component.serialization.impl.AbilityContainerSerializer;
import me.mykindos.betterpvp.core.item.component.serialization.impl.StatContainerSerializer;
import me.mykindos.betterpvp.core.item.component.serialization.impl.UUIDPropertySerializer;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for managing component serializers and deserializers.
 * This centralizes the registration and lookup of serialization handlers.
 */
@Singleton
public class ComponentSerializationRegistry {
    private final Map<Class<? extends ItemComponent>, ComponentSerializer<?>> serializers = new HashMap<>();

    private final Map<NamespacedKey, ComponentDeserializer<?>> deserializers = new HashMap<>();

    /**
     * Register a serializer for a component type.
     *
     * @param serializer The serializer to register
     * @param <T> The component type
     */
    public <T extends ItemComponent> void registerSerializer(@NotNull ComponentSerializer<T> serializer) {
        serializers.put(serializer.getType(), serializer);
    }

    @Inject
    private void registerDefaultSerializers(StatSerializationRegistry statRegistry, ItemAbilitySerializationRegistry itemAbilitySerializationRegistry) {
        register(new UUIDPropertySerializer());
        register(new StatContainerSerializer(statRegistry));
        register(new AbilityContainerSerializer(itemAbilitySerializationRegistry));
        register(new FuelComponentSerializer());
    }

    /**
     * Register a deserializer for a component type.
     *
     * @param deserializer The deserializer to register
     * @param <T> The component type
     */
    public <T extends ItemComponent> void registerDeserializer(@NotNull ComponentDeserializer<T> deserializer) {
        deserializers.put(deserializer.getKey(), deserializer);
    }

    /**
     * Register a combined serializer/deserializer.
     *
     * @param handler The handler that implements both interfaces
     * @param <T> The component type
     */
    public <T extends ItemComponent> void register(@NotNull ComponentSerializer<T> handler) {
        registerSerializer(handler);
        if (handler instanceof ComponentDeserializer) {
            @SuppressWarnings("unchecked")
            ComponentDeserializer<T> deserializer = (ComponentDeserializer<T>) handler;
            registerDeserializer(deserializer);
        }
    }

    /**
     * Get a serializer for a component type.
     *
     * @param componentType The component type
     * @return Optional containing the serializer if found
     */
    @SuppressWarnings("unchecked")
    public <T extends ItemComponent> Optional<ComponentSerializer<T>> getSerializer(@NotNull Class<T> componentType) {
        ComponentSerializer<?> serializer = serializers.get(componentType);
        return Optional.ofNullable((ComponentSerializer<T>) serializer);
    }

    /**
     * Get a deserializer for a component key.
     *
     * @param key The component key
     * @return Optional containing the deserializer if found
     */
    public Optional<ComponentDeserializer<?>> getDeserializer(@NotNull NamespacedKey key) {
        return Optional.ofNullable(deserializers.get(key));
    }

    /**
     * Get all registered deserializers.
     *
     * @return Map of all deserializers
     */
    public Map<NamespacedKey, ComponentDeserializer<?>> getAllDeserializers() {
        return Map.copyOf(deserializers);
    }

    /**
     * Check if a component type can be serialized.
     *
     * @param componentType The component type to check
     * @return True if a serializer is registered for this type
     */
    public boolean canSerialize(@NotNull Class<? extends ItemComponent> componentType) {
        return serializers.containsKey(componentType);
    }

    /**
     * Check if a component key can be deserialized.
     *
     * @param key The component key to check
     * @return True if a deserializer is registered for this key
     */
    public boolean canDeserialize(@NotNull NamespacedKey key) {
        return deserializers.containsKey(key);
    }
}