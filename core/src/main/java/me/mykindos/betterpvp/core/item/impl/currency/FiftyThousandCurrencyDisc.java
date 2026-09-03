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
@ItemKey("core:fifty_thousand_disc")
@FallbackItem(value = Material.MUSIC_DISC_13)
public class FiftyThousandCurrencyDisc extends BaseItem {

    private static final int VALUE = 50_000;

    public FiftyThousandCurrencyDisc() {
        super("Fifty Thousand Disc", ItemStack.of(Material.MUSIC_DISC_13), ItemGroup.MISC, ItemRarity.UNCOMMON);

        addSerializableComponent(new CurrencyDiscComponent(VALUE));
    }
}