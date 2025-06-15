package me.mykindos.betterpvp.core.item.component.impl.runes.scorching;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRune;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class ScorchingRuneItem extends RuneItem {

    @Inject
    private ScorchingRuneItem(ScorchingRune rune) {
        super(rune, ItemStack.of(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE), ItemRarity.EPIC);
    }
}
