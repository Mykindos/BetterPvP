package me.mykindos.betterpvp.clans.fields.model;

import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.entity.Item;
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
    default boolean processInteraction(TerritoryInteractEvent event, FieldsBlock block, EffectManager effectManager) {
        if (!event.getInteractionType().equals(TerritoryInteractEvent.InteractionType.BREAK)) {
            return false; // They didn't break the ore
        }

        final Player player = event.getPlayer();
        boolean isProtected = effectManager.hasEffect(player, EffectTypes.PROTECTION);

        // Drop the items
        final ItemStack[] itemStacks = generateDrops(block);
        for (ItemStack itemStack : itemStacks) {
            Item item = event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), itemStack);
            if (isProtected) {
                UtilItem.reserveItem(item, player, 10);
            }
        }
        return true;
    }
}
