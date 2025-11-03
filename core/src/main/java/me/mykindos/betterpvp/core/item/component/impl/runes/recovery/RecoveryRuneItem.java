package me.mykindos.betterpvp.core.item.component.impl.runes.recovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class RecoveryRuneItem extends RuneItem {

    @Inject
    private RecoveryRuneItem(RecoveryRune rune) {
        super(rune, RuneColor.ARMOR, ItemRarity.EPIC);
    }
}
