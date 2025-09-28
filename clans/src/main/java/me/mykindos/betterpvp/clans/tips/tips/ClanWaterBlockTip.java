package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRuneItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class ClanWaterBlockTip extends ClanTip {

    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private BaseItem waterBlock = null;

    @Inject
    protected ClanWaterBlockTip(Clans clans, ItemFactory itemFactory, ItemRegistry itemRegistry) {
        super(clans, 1, 2);
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public Component generateComponent() {
        waterBlock = itemRegistry.getItem(new NamespacedKey("clans", "water_block"));
        if (waterBlock == null) {
            return Component.empty();
        }

        final ItemInstance instance = itemFactory.create(waterBlock);
        return Component.empty().append(Component.text("You can place water in your territory by buying or crafting a "))
                .append(instance.getView().getName()).hoverEvent(instance.getView().get().asHoverEvent())
                .append(Component.text(" (lapis block), then placing it like you would a water bucket"));
    }

    @Override
    public String getName() {
        return "waterblock";
    }

    @Override
    public boolean isValid(Player player, Clan clan) {
        if (waterBlock == null) {
            UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                setComponent(generateComponent());
            });
        }
        return waterBlock != null;
        // todo: add enabling condition for water block
//        return waterBlock != null && waterBlock.isEnabled();
    }
}
