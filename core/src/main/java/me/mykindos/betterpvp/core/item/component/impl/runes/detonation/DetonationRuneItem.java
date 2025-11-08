package me.mykindos.betterpvp.core.item.component.impl.runes.detonation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class DetonationRuneItem extends RuneItem {

    @Inject
    private DetonationRuneItem(DetonationRune rune) {
        super(rune, RuneColor.WEAPON, ItemRarity.EPIC);
    }
}
