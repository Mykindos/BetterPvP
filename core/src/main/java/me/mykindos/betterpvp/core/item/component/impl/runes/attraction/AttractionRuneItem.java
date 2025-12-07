package me.mykindos.betterpvp.core.item.component.impl.runes.attraction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
@ItemKey("core:attraction_rune")
public class AttractionRuneItem extends RuneItem {

    @Inject
    private AttractionRuneItem(AttractionRune rune) {
        super(rune, RuneColor.ARMOR, ItemRarity.RARE);
    }
}
