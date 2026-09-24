package me.mykindos.betterpvp.core.world.construction.blueprint;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Keeps which structure a blueprint places, and which placed structure it moves if any, on the item itself. */
public class StructureBlueprintSerializer implements ComponentSerializer<StructureBlueprintComponent>,
        ComponentDeserializer<StructureBlueprintComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "structure_blueprint");
    private static final NamespacedKey MOVING_KEY = new NamespacedKey("betterpvp", "structure_blueprint_moving");

    @Override
    public @NotNull Class<StructureBlueprintComponent> getType() {
        return StructureBlueprintComponent.class;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public void serialize(@NotNull StructureBlueprintComponent instance, @NotNull PersistentDataContainer container) {
        container.set(KEY, PersistentDataType.STRING, instance.getStructure());
        if (instance.getMoving() == null) {
            container.remove(MOVING_KEY);
        } else {
            container.set(MOVING_KEY, PersistentDataType.STRING, instance.getMoving().toString());
        }
    }

    @Override
    public void delete(@NotNull StructureBlueprintComponent instance, @NotNull PersistentDataContainer container) {
        container.remove(KEY);
        container.remove(MOVING_KEY);
    }

    @Override
    public @NotNull StructureBlueprintComponent deserialize(@NotNull ItemInstance item,
                                                            @NotNull PersistentDataContainer container) {
        final String structure = container.get(KEY, PersistentDataType.STRING);
        final String moving = container.get(MOVING_KEY, PersistentDataType.STRING);
        return new StructureBlueprintComponent(structure == null ? "" : structure, parse(moving));
    }

    private static @Nullable UUID parse(@Nullable String moving) {
        if (moving == null) {
            return null;
        }
        try {
            return UUID.fromString(moving);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
