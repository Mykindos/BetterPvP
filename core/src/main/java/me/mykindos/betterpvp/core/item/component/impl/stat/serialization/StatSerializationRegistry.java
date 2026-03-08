package me.mykindos.betterpvp.core.item.component.impl.stat.serialization;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypeRegistry;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypes;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.impl.DoubleStatSerializer;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.impl.IntegerStatSerializer;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for managing stat serializers and deserializers.
 * This centralizes the registration and lookup of stat serialization handlers.
 */
@Singleton
public class StatSerializationRegistry {

    // Map value types (Double.class, Integer.class) to their serializers
    private final Map<Class<?>, StatSerializer<?>> serializersByValueType = new HashMap<>();
    private final Map<NamespacedKey, StatDeserializer<?>> deserializers = new HashMap<>();
    private final StatTypeRegistry typeRegistry;

    // Singleton serializers
    private final DoubleStatSerializer doubleSerializer;
    private final IntegerStatSerializer integerSerializer;

    @Inject
    public StatSerializationRegistry(StatTypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;

        // Create singleton serializers
        this.doubleSerializer = new DoubleStatSerializer(typeRegistry);
        this.integerSerializer = new IntegerStatSerializer(typeRegistry);

        registerDefaultSerializers();
    }

    /**
     * Get a serializer for a stat based on its value type.
     *
     * @param valueType The value type (Double.class, Integer.class)
     * @return The serializer for this value type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> StatSerializer<ItemStat<T>> getSerializerForValueType(@NotNull Class<T> valueType) {
        StatSerializer<?> serializer = serializersByValueType.get(valueType);
        if (serializer == null) {
            throw new IllegalArgumentException("No serializer registered for value type: " + valueType);
        }
        return (StatSerializer) serializer;
    }

    /**
     * Get a serializer for a stat based on the stat instance.
     * Determines the value type from the stat's type.
     *
     * @param stat The stat instance
     * @return The serializer for this stat
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> StatSerializer<ItemStat<T>> getSerializer(@NotNull ItemStat<T> stat) {
        // Determine value type from the stat's actual value
        Object value = stat.getValue();
        if (value instanceof Double) {
            return (StatSerializer) doubleSerializer;
        } else if (value instanceof Integer) {
            return (StatSerializer) integerSerializer;
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
        }
    }

    /**
     * Get a deserializer for a stat key.
     * 
     * @param key The stat key
     * @return Optional containing the deserializer if found
     */
    public Optional<StatDeserializer<?>> getDeserializer(@NotNull NamespacedKey key) {
        return Optional.ofNullable(deserializers.get(key));
    }

    /**
     * Get all registered deserializers.
     * 
     * @return Map of all deserializers
     */
    public Map<NamespacedKey, StatDeserializer<?>> getAllDeserializers() {
        return Map.copyOf(deserializers);
    }

    /**
     * Check if a stat key can be deserialized.
     *
     * @param key The stat key to check
     * @return True if a deserializer is registered for this key
     */
    public boolean canDeserialize(@NotNull NamespacedKey key) {
        return deserializers.containsKey(key);
    }

    private void registerDefaultSerializers() {
        // Register singleton serializers by value type
        serializersByValueType.put(Double.class, doubleSerializer);
        serializersByValueType.put(Integer.class, integerSerializer);

        // Register deserializers for each stat type that uses these value types
        // Double-based stats
        deserializers.put(StatTypes.MELEE_DAMAGE.getKey(), doubleSerializer);
        deserializers.put(StatTypes.MELEE_ATTACK_SPEED.getKey(), doubleSerializer);
        deserializers.put(StatTypes.MOVEMENT.getKey(), doubleSerializer);

        // Integer-based stats
        deserializers.put(StatTypes.HEALTH.getKey(), integerSerializer);
    }
} 