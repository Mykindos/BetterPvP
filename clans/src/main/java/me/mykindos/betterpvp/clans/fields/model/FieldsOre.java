package me.mykindos.betterpvp.clans.fields.model;

import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
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
    default boolean processInteraction(TerritoryInteractEvent event, FieldsBlock block) {
        if (!event.getInteractionType().equals(TerritoryInteractEvent.InteractionType.BREAK)) {
            return false; // They didn't break the ore
        }

        // Drop the items
        final ItemStack[] itemStacks = generateDrops(block);
        for (ItemStack itemStack : itemStacks) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), itemStack);
        }
        return true;
    }
}
