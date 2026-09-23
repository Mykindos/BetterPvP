package me.mykindos.betterpvp.core.world.construction.blueprint;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/** Keeps which structure a blueprint places on the item itself. */
public class StructureBlueprintSerializer implements ComponentSerializer<StructureBlueprintComponent>,
        ComponentDeserializer<StructureBlueprintComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "structure_blueprint");

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
    }

    @Override
    public void delete(@NotNull StructureBlueprintComponent instance, @NotNull PersistentDataContainer container) {
        container.remove(KEY);
    }

    @Override
    public @NotNull StructureBlueprintComponent deserialize(@NotNull ItemInstance item,
                                                            @NotNull PersistentDataContainer container) {
        final String structure = container.get(KEY, PersistentDataType.STRING);
        return new StructureBlueprintComponent(structure == null ? "" : structure);
    }
}
