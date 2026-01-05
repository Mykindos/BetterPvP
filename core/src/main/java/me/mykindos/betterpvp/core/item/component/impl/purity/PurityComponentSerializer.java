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
 * Stores purity as a string (enum name) and attuned state as a byte in the PersistentDataContainer.
 */
public class PurityComponentSerializer implements ComponentSerializer<PurityComponent>, ComponentDeserializer<PurityComponent> {

    private static final NamespacedKey PURITY_KEY = new NamespacedKey("betterpvp", "purity");
    private static final NamespacedKey ATTUNED_KEY = new NamespacedKey("betterpvp", "purity_attuned");

    @Override
    @NotNull
    public Class<PurityComponent> getType() {
        return PurityComponent.class;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return PURITY_KEY;
    }

    @Override
    public void serialize(@NotNull PurityComponent instance, @NotNull PersistentDataContainer container) {
        // Serialize purity enum as string
        container.set(PURITY_KEY, PersistentDataType.STRING, instance.getPurity().name());

        // Serialize attuned state as byte (1 = attuned, 0 = not attuned)
        container.set(ATTUNED_KEY, PersistentDataType.BYTE, instance.isAttuned() ? (byte) 1 : (byte) 0);
    }

    @Override
    public void delete(@NotNull PurityComponent instance, @NotNull PersistentDataContainer container) {
        if (container.has(PURITY_KEY, PersistentDataType.STRING)) {
            container.remove(PURITY_KEY);
        }
        if (container.has(ATTUNED_KEY, PersistentDataType.BYTE)) {
            container.remove(ATTUNED_KEY);
        }
    }

    @Override
    public @NotNull PurityComponent deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        // Deserialize purity
        Preconditions.checkArgument(container.has(PURITY_KEY, PersistentDataType.STRING), "Container does not have purity data");
        String purityName = container.get(PURITY_KEY, PersistentDataType.STRING);
        Preconditions.checkNotNull(purityName, "Purity data is null");

        try {
            ItemPurity purity = ItemPurity.valueOf(purityName);

            // Deserialize attuned state (default to false if not present for backward compatibility)
            Byte attunedByte = container.get(ATTUNED_KEY, PersistentDataType.BYTE);
            boolean attuned = attunedByte != null && attunedByte == 1;

            return new PurityComponent(purity, attuned);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid purity value: " + purityName, e);
        }
    }
}
