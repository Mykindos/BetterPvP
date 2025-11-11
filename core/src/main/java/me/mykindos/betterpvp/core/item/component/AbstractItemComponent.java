package me.mykindos.betterpvp.core.item.component;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractItemComponent implements ItemComponent {

    private final NamespacedKey namespacedKey;

    protected AbstractItemComponent(String key) {
        this.namespacedKey = new NamespacedKey("betterpvp", key);
    }

    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return namespacedKey;
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
