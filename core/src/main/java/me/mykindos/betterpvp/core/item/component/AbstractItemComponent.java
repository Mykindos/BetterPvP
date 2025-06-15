package me.mykindos.betterpvp.core.item.component;

import me.mykindos.betterpvp.core.item.Item;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractItemComponent implements ItemComponent {

    private final NamespacedKey namespacedKey;
    private final boolean allowsDuplicates;

    protected AbstractItemComponent(String key, boolean allowsDuplicates) {
        this.namespacedKey = new NamespacedKey("betterpvp", key);
        this.allowsDuplicates = allowsDuplicates;
    }

    protected AbstractItemComponent(String key) {
        this(key, false);
    }

    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    @Override
    public boolean isCompatibleWith(@NotNull Item item) {
        if (!allowsDuplicates) {
            // Check if the item already has this component
            return item.getComponents(getClass()).isEmpty();
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        AbstractItemComponent that = (AbstractItemComponent) o;
        return namespacedKey.equals(that.namespacedKey);
    }

    @Override
    public int hashCode() {
        // namespacedkey has different hashCodes despite being equal, so we combine the namespace and key
        return namespacedKey.getKey().hashCode() + namespacedKey.getNamespace().hashCode();
    }
}
