package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.wanderer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.socketables.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.socketables.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
@ItemKey("core:wanderer_rune")
public class WandererRuneItem extends RuneItem {

    @Inject
    private WandererRuneItem(WandererRune rune) {
        super(rune, RuneColor.ARMOR, ItemRarity.RARE);
    }
}
