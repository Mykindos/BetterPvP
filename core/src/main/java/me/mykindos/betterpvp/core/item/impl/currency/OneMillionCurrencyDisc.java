package me.mykindos.betterpvp.core.item.impl.currency;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.currency.disc.CurrencyDiscComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("core:one_million_disc")
@FallbackItem(value = Material.MUSIC_DISC_PIGSTEP)
public class OneMillionCurrencyDisc extends BaseItem {

    private static final int VALUE = 1_000_000;

    public OneMillionCurrencyDisc() {
        super("One Million Disc", ItemStack.of(Material.MUSIC_DISC_PIGSTEP), ItemGroup.MISC, ItemRarity.UNCOMMON);

        addSerializableComponent(new CurrencyDiscComponent(VALUE));
    }
}