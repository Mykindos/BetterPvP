package me.mykindos.betterpvp.core.item.component.impl.runes.namelesscloak;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneColor;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;

@Singleton
@EqualsAndHashCode(callSuper = false)
@ItemKey("core:nameless_cloak_rune")
public class NamelessCloakRuneItem extends RuneItem {

    @Inject
    private NamelessCloakRuneItem(NamelessCloakRune rune) {
        super(rune, RuneColor.ARMOR, ItemRarity.RARE);
    }
}
