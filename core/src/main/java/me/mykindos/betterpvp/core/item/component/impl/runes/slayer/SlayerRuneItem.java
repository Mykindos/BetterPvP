package me.mykindos.betterpvp.core.item.component.impl.runes.slayer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class SlayerRuneItem extends RuneItem {

    @Inject
    private SlayerRuneItem(SlayerRune rune) {
        super(rune, RuneColor.WEAPON, ItemRarity.LEGENDARY);
    }
}
