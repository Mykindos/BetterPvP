package me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class UnbreakingRuneItem extends RuneItem {

    @Inject
    private UnbreakingRuneItem(UnbreakingRune rune) {
        super(rune, ItemStack.of(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE), ItemRarity.LEGENDARY);
    }
}
