package me.mykindos.betterpvp.clans.clans.leveling.perk.model;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkCategory;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

@Getter
public class ClanFarmingLevels implements ClanPerk {

    private final String perkId;
    private final int levels;
    private final int minReq;

    public ClanFarmingLevels(String perkId, int levels, int minReq) {
        this.perkId = perkId;
        this.levels = levels;
        this.minReq = minReq;
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
        return new Component[]{
                Component.text("Allows your clan to make their farm " + levels + " levels deeper.", NamedTextColor.GRAY)
        };
    }

    @Override
    public ItemView getIcon() {
        return ItemView.builder().material(Material.DIAMOND_HOE).build();
    }

    @Override
    public ClanPerkCategory getCategory() {
        return ClanPerkCategory.FARMING;
    }

}
