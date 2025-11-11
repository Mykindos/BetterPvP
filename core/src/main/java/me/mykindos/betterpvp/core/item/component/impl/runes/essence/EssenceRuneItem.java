package me.mykindos.betterpvp.core.item.component.impl.runes.essence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
@ItemKey("core:essence_rune")
public class EssenceRuneItem extends RuneItem {

    @Inject
    private EssenceRuneItem(EssenceRune rune) {
        super(rune, RuneColor.WEAPON, ItemRarity.UNCOMMON);
    }
}
