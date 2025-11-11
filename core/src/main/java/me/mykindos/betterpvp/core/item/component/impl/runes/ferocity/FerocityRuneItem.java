package me.mykindos.betterpvp.core.item.component.impl.runes.ferocity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
@ItemKey("core:ferocity_rune")
public class FerocityRuneItem extends RuneItem {

    @Inject
    private FerocityRuneItem(FerocityRune rune) {
        super(rune, RuneColor.WEAPON, ItemRarity.RARE);
    }
}
