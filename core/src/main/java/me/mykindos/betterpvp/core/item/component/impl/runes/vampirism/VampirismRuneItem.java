package me.mykindos.betterpvp.core.item.component.impl.runes.vampirism;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@ItemKey("core:vampirism_rune")
@EqualsAndHashCode(callSuper = false)
public class VampirismRuneItem extends RuneItem {

    @Inject
    private VampirismRuneItem(VampirismRune rune) {
        super(rune, RuneColor.WEAPON, ItemRarity.LEGENDARY);
    }
}
