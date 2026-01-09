package me.mykindos.betterpvp.core.item.component.impl.currency;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:gold_bars")
public class GoldBars extends BaseItem {

    @Inject
    private GoldBars() {
        super("Gold Bars", Item.model("gold_bars", 99), ItemGroup.MATERIAL, ItemRarity.UNCOMMON);
        addBaseComponent(new CurrencyComponent(1));
    }
}
