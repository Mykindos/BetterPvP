package me.mykindos.betterpvp.core.item.component.impl.runes.brutality;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
@ItemKey("core:brutality_rune")
public class BrutalityRuneItem extends RuneItem {

    @Inject
    private BrutalityRuneItem(BrutalityRune rune) {
        super(rune, RuneColor.WEAPON, ItemRarity.UNCOMMON);
    }
}
