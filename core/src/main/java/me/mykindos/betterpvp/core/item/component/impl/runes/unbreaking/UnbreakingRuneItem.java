package me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
@ItemKey("core:unbreaking_rune")
public class UnbreakingRuneItem extends RuneItem {

    @Inject
    private UnbreakingRuneItem(UnbreakingRune rune) {
        super(rune, RuneColor.MISC, ItemRarity.MYTHICAL);
    }
}
