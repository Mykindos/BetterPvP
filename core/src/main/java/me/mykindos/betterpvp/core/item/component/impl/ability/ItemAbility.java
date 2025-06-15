package me.mykindos.betterpvp.core.item.component.impl.ability;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an ability for an item.
 */
@Getter
public abstract class ItemAbility {

    private final NamespacedKey key;
    private final String name;
    private final String description;
    private final TriggerType triggerType;
    @Setter
    private boolean consumesItem = false;

    /**
     * Creates a new ability with the given properties.
     * The key will be created using the namespace "betterpvp" and a sanitized version of the name.
     *
     * @param name The ability name
     * @param description The ability description
     * @param triggerType The trigger type
     */
    public ItemAbility(@NotNull NamespacedKey key, @NotNull String name, @NotNull String description, @NotNull TriggerType triggerType) {
        Preconditions.checkArgument(!name.isEmpty(), "Name cannot be empty");
        Preconditions.checkArgument(!description.isEmpty(), "Description cannot be empty");
        this.key = key;
        this.name = name;
        this.description = description;
        this.triggerType = triggerType;
    }

    /**
     * Invokes the ability.
     *
     * @param client       the client that invoked the ability
     * @param itemInstance the item instance that invoked the ability
     * @param itemStack    the item stack that invoked the ability
     * @return true if the ability was successfully invoked, false otherwise
     */
    public abstract boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack);
}
