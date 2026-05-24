package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.forestwright;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.socketables.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.socketables.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
@ItemKey("core:forestwright_rune")
public class ForestwrightRuneItem extends RuneItem {

    @Inject
    private ForestwrightRuneItem(ForestwrightRune rune) {
        super(rune, RuneColor.TOOL, ItemRarity.LEGENDARY);
    }
}
