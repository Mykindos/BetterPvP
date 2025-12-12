package me.mykindos.betterpvp.core.item.component.impl.purity;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Serializer and deserializer for PurityComponent.
 * Stores purity as a string (enum name) in the PersistentDataContainer.
 */
public class PurityComponentSerializer implements ComponentSerializer<PurityComponent>, ComponentDeserializer<PurityComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "purity");

    @Override
    @NotNull
    public Class<PurityComponent> getType() {
        return PurityComponent.class;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public void serialize(@NotNull PurityComponent instance, @NotNull PersistentDataContainer container) {
        container.set(KEY, PersistentDataType.STRING, instance.getPurity().name());
    }

    @Override
    public void delete(@NotNull PurityComponent instance, @NotNull PersistentDataContainer container) {
        if (container.has(KEY, PersistentDataType.STRING)) {
            container.remove(KEY);
        }
    }

    @Override
    public @NotNull PurityComponent deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        Preconditions.checkArgument(container.has(KEY, PersistentDataType.STRING), "Container does not have purity data");
        String purityName = container.get(KEY, PersistentDataType.STRING);
        Preconditions.checkNotNull(purityName, "Purity data is null");

        try {
            ItemPurity purity = ItemPurity.valueOf(purityName);
            return new PurityComponent(purity);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid purity value: " + purityName, e);
        }
    }
}
