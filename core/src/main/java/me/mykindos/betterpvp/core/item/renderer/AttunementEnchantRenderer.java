package me.mykindos.betterpvp.core.item.renderer;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.purity.PurityComponent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Adds an enchantment glint to attuned items that do not have a custom model.
 */
public class AttunementEnchantRenderer implements ItemStackRenderer {

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void write(ItemInstance item, ItemStack itemStack) {
        if (item.getRarity().getImportance() < ItemRarity.EPIC.getImportance()) {
            final Optional<PurityComponent> purityComponent = item.getComponent(PurityComponent.class);
            if (purityComponent.isPresent() && purityComponent.get().isAttuned()) {
                itemStack.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            }
        }

    }
}
