package me.mykindos.betterpvp.clans.clans.leveling.perk.model;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.UUID;

@Getter
public class ClanFarmingLevels implements ClanPerk {

    private final UUID perkUUID;
    private final int levels;
    private final int minReq;

    public ClanFarmingLevels(int levels, int minReq) {
        this.levels = levels;
        this.minReq = minReq;
        this.perkUUID = UUID.randomUUID();
    }

    @Override
    public String getName() {
        return "+" + levels + " Base Farming Levels";
    }

    @Override
    public int getMinimumLevel() {
        return minReq;
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                Component.text("Allows your clan to make their farm " + levels + " levels deeper.", NamedTextColor.GRAY)
        };
    }

    @Override
    public ItemView getIcon() {
        return ItemView.builder().material(Material.DIAMOND_HOE).build();
    }

}
