package me.mykindos.betterpvp.champions.weapons.impl.runes.haste;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.champions.weapons.impl.runes.SingleStatRune;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public abstract class HasteBase extends SingleStatRune {

    protected HasteBase(Champions plugin, String key) {
        super(plugin, key);
    }

    @Override
    public List<Component> getRuneLoreDescription(ItemMeta itemMeta) {
        double roll = getRollFromMeta(itemMeta);
        return List.of(UtilMessage.deserialize("<gray>Increases attack speed by <green>%.1f%%",  roll));
    }

    @Override
    public List<Component> getItemLoreDescription(PersistentDataContainer pdc, ItemStack itemStack) {
        int tier = pdc.getOrDefault(RuneNamespacedKeys.TIER, PersistentDataType.INTEGER, 0);
        double roll = pdc.getOrDefault(getAppliedNamespacedKey(), PersistentDataType.DOUBLE, 0.0);
        return List.of(UtilMessage.deserialize("%s <gray>- <green>%.1f%% <reset>increased attack speed", getStarPrefix(tier),  roll));
    }

    @Override
    public String[] getItemFilter() {
        return Rune.MELEE_WEAPON_FILTER;
    }

    @Override
    public String getCategory() {
        return "melee weapons";
    }

    @Override
    public NamespacedKey getAppliedNamespacedKey() {
        return RuneNamespacedKeys.HASTE;
    }



}
