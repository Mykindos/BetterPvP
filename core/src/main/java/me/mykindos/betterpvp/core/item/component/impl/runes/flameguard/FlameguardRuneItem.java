package me.mykindos.betterpvp.core.item.component.impl.runes.flameguard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
@ItemKey("core:flameguard_rune")
public class FlameguardRuneItem extends RuneItem {

    @Inject
    private FlameguardRuneItem(FlameguardRune rune) {
        super(rune, RuneColor.ARMOR, ItemRarity.UNCOMMON);
    }
}
