package me.mykindos.betterpvp.clans.fields.model;

import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The different types of ores that can be found in the Fields clan.
 * These are the main ore families that can be found.
 */
public interface FieldsOre extends FieldsInteractable {

    /**
     * The drops to give when the ore is mined.
     * If the field is null, the default drops will be given.
     */
    @NotNull ItemStack @NotNull [] generateDrops(final @NotNull FieldsBlock fieldsBlock);

    @Override
    default boolean processInteraction(TerritoryInteractEvent event, FieldsBlock block, ItemFactory itemFactory) {
        if (!event.getInteractionType().equals(TerritoryInteractEvent.InteractionType.BREAK)) {
            return false; // They didn't break the ore
        }

        final Player player = event.getPlayer();

        // Drop the items
        final ItemStack[] itemStacks = generateDrops(block);
        for (ItemStack itemStack : itemStacks) {
            UtilItem.insert(player, itemFactory.convertItemStack(itemStack).orElse(itemStack));
        }
        return true;
    }
}
