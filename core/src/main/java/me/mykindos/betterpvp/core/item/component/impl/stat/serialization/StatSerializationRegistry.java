package me.mykindos.betterpvp.core.item.component.impl.stat.serialization;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.repo.MeleeDamageStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.repo.MeleeAttackSpeedStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.repo.MovementStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.impl.DoubleStatSerializer;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.impl.IntegerStatSerializer;
import me.mykindos.betterpvp.core.item.component.impl.stat.type.DoubleItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.type.IntegerItemStat;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Registry for managing stat serializers and deserializers.
 * This centralizes the registration and lookup of stat serialization handlers.
 */
@Singleton
public class StatSerializationRegistry {

    private final Map<Class<? extends ItemStat<?>>, StatSerializer<?>> serializers = new HashMap<>();
    private final Map<NamespacedKey, StatDeserializer<?>> deserializers = new HashMap<>();

    @Inject
    public StatSerializationRegistry() {
        registerDefaultSerializers();
    }

    /**
     * Register a serializer for a stat type.
     * 
     * @param serializer The serializer to register
     * @param <T> The stat type
     */
    public <T extends ItemStat<?>> void registerSerializer(@NotNull StatSerializer<T> serializer) {
        serializers.put(serializer.getType(), serializer);
    }

    /**
     * Register a deserializer for a stat type.
     * 
     * @param deserializer The deserializer to register
     * @param <T> The stat type
     */
    public <T extends ItemStat<?>> void registerDeserializer(@NotNull StatDeserializer<T> deserializer) {
        deserializers.put(deserializer.getKey(), deserializer);
    }

    /**
     * Register a combined serializer/deserializer.
     * 
     * @param handler The handler that implements both interfaces
     * @param <T> The stat type
     */
    public <T extends ItemStat<?>> void register(@NotNull StatSerializer<T> handler) {
        registerSerializer(handler);
        if (handler instanceof StatDeserializer) {
            @SuppressWarnings("unchecked")
            StatDeserializer<T> deserializer = (StatDeserializer<T>) handler;
            registerDeserializer(deserializer);
        }
    }

    /**
     * Convenient method to register a DoubleItemStat type.
     * 
     * @param key The namespaced key for this stat type
     * @param statType The stat class
     * @param constructor Function to create stat instances from double modifiers
     * @param <T> The specific DoubleItemStat type
     */
    public <T extends DoubleItemStat> void registerDoubleStat(@NotNull NamespacedKey key, @NotNull Class<T> statType, @NotNull Function<Double, T> constructor) {
        register(new DoubleStatSerializer<>(key, statType, constructor));
    }

    /**
     * Convenient method to register an IntegerItemStat type.
     * 
     * @param key The namespaced key for this stat type
     * @param statType The stat class
     * @param constructor Function to create stat instances from integer modifiers
     * @param <T> The specific IntegerItemStat type
     */
    public <T extends IntegerItemStat> void registerIntegerStat(@NotNull NamespacedKey key, @NotNull Class<T> statType, @NotNull Function<Integer, T> constructor) {
        register(new IntegerStatSerializer<>(key, statType, constructor));
    }

    /**
     * Get a serializer for a stat type.
     * 
     * @param statType The stat type
     * @return Optional containing the serializer if found
     */
    @SuppressWarnings("unchecked")
    public <T extends ItemStat<?>> Optional<StatSerializer<T>> getSerializer(@NotNull Class<T> statType) {
        StatSerializer<?> serializer = serializers.get(statType);
        return Optional.ofNullable((StatSerializer<T>) serializer);
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
     * Check if a stat type can be serialized.
     * 
     * @param statType The stat type to check
     * @return True if a serializer is registered for this type
     */
    public boolean canSerialize(@NotNull Class<? extends ItemStat<?>> statType) {
        return serializers.containsKey(statType);
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
        // Register double-based stats using the convenient helper method
        registerDoubleStat(
            new NamespacedKey("betterpvp", "melee-damage"),
            MeleeDamageStat.class,
            MeleeDamageStat::new
        );

        registerDoubleStat(
            new NamespacedKey("betterpvp", "move-speed"),
            MovementStat.class,
            MovementStat::new
        );

        registerDoubleStat(
            new NamespacedKey("betterpvp", "melee-attack-speed"),
            MeleeAttackSpeedStat.class,
            MeleeAttackSpeedStat::new
        );
    }
} 