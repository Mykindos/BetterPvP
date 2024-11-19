package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@Slf4j
public class ClanWaterBlockTip extends ClanTip {
    private final ItemHandler itemHandler;
    private BPvPItem waterBlock = null;
    @Inject
    protected ClanWaterBlockTip(Clans clans, ItemHandler itemHandler) {
        super(clans, 1, 2);
        this.itemHandler = itemHandler;
    }

    @Override
    public Component generateComponent() {
        waterBlock = itemHandler.getItem("clans:water_block");
        if (waterBlock == null) return Component.empty();
        return Component.empty().append(Component.text("You can place water in your territory by buying or crafting a "))
                .append(waterBlock.getName()).hoverEvent(waterBlock.getItemStack().asHoverEvent())
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
        return waterBlock != null && waterBlock.isEnabled();
    }
}
