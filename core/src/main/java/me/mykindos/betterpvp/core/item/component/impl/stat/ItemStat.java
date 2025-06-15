package me.mykindos.betterpvp.core.item.component.impl.stat;

import com.google.common.base.Preconditions;
import lombok.Getter;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import me.mykindos.betterpvp.core.item.Item;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Represents a statistical attribute for an item.
 * This can include various properties such as damage, speed, etc.
 * <p>
 * Each statistic has an associated bukkit {@link org.bukkit.event.Listener} or handler class that
 * will manage its behavior and interactions within the game. Their instantiation and, hence, execution
 * order is determined by Guice, which is a dependency injection framework.
 * <p>
 * ItemStats are immutable and contained within a {@link StatContainerComponent}.
 * Serialization is handled by the StatSerializationRegistry.
 */
@Getter
public abstract class ItemStat<T> {

    public static final TextColor RED = TextColor.color(255, 0, 0);
    public static final TextColor GREEN = TextColor.color(0, 255, 0);
    public static final TextColor BLUE = TextColor.color(0, 0, 255);

    private final NamespacedKey key;
    private final String name;
    private final String shortName;
    private final String description;
    private final T value;

    protected ItemStat(String keyName, String name, String shortName, String description, T value) {
        this.key = new NamespacedKey("betterpvp", keyName.toLowerCase().replace("_", "-"));
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        Preconditions.checkArgument(isValidValue(value), "value is not valid");
        this.value = value;
    }

    public ItemStat(String keyName, String name, String description, TextColor valueColor, T value) {
        this(keyName, name, name, description, value);
    }

    public void onApply(Item item, ItemStack stack) {
        // Default implementation does nothing
    }

    public void onRemove(Item item, ItemStack stack) {
        // Default implementation does nothing
    }

    protected boolean isValidValue(T value) {
        return value != null;
    }

    protected abstract TextColor getValueColor();

    public abstract String stringValue();

    public abstract ItemStat<T> copy();

    /**
     * Creates a new instance of this stat with a different value.
     * This is the primary way to "edit" an immutable stat.
     * 
     * @param newValue The new value for the stat
     * @return A new ItemStat instance with the updated value
     */
    public abstract ItemStat<T> withValue(T newValue);

    /**
     * Merge this stat with another stat of the same type.
     * By default, throws UnsupportedOperationException. Subclasses should override.
     *
     * @param other The other stat to merge with
     * @return A new merged ItemStat
     */
    public ItemStat<T> merge(ItemStat<?> other) {
        throw new UnsupportedOperationException("Merging not supported for this stat type: " + getClass());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ItemStat<?> itemStat = (ItemStat<?>) o;
        return Objects.equals(key, itemStat.key) && Objects.equals(value, itemStat.value);
    }

    @Override
    public int hashCode() {
        // hashcode of key and namespace and value
        int result = key.getNamespace().hashCode() * 31 + key.getKey().hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
