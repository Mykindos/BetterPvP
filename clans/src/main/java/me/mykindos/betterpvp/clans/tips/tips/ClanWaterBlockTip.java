package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.item.WaterBlock;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Locale;

@Singleton
public class ClanWaterBlockTip extends ClanTip {

    private final ItemFactory itemFactory;
    private final WaterBlock waterBlock;

    @Inject
    protected ClanWaterBlockTip(Clans clans, ItemFactory itemFactory, WaterBlock waterBlock) {
        super(clans, 1, 2);
        this.itemFactory = itemFactory;
        this.waterBlock = waterBlock;
        setComponent(generateComponent());
    }

    @Override
    public Component generateComponent() {

        final ItemInstance instance = itemFactory.createPreview(waterBlock);
        // This tip component is built once and shown to many players, so the hover item is resolved to
        // English (the universal fallback); the inline name still renders per-viewer when sent.
        final Component itemComponent = instance.getView().getName()
                .hoverEvent(Translations.renderItemStack(instance.getView().get(), Locale.ENGLISH).asHoverEvent());
        return Translations.component("clans.tip.water-block", itemComponent);
    }

    @Override
    public String getName() {
        return "waterblock";
    }

    @Override
    public boolean isValid(Player player, Clan clan) {
        return clan != null;
    }
}
