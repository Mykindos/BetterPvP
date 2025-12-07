package me.mykindos.betterpvp.core.item.component.impl.durability;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.renderer.ItemStackRenderer;
import org.bukkit.inventory.ItemStack;

/**
 * @see DurabilityComponent
 */
public class DurabilityRenderer implements ItemStackRenderer {

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void write(ItemInstance item, ItemStack itemStack) {
        item.getComponent(DurabilityComponent.class).ifPresent(durabilityComponent -> {
            final int maxDamage = durabilityComponent.getMaxDamage();
            final int damage = durabilityComponent.getDamage();
            itemStack.setData(DataComponentTypes.MAX_DAMAGE, maxDamage);
            itemStack.setData(DataComponentTypes.DAMAGE, damage);
        });
    }
}
