package me.mykindos.betterpvp.core.item.component.impl.runes.stonecaller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class StonecallerRuneItem extends RuneItem {

    @Inject
    private StonecallerRuneItem(StonecallerRune rune) {
        super(rune, RuneColor.TOOL, ItemRarity.LEGENDARY);
    }
}
