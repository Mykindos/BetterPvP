package me.mykindos.betterpvp.champions.weapons.impl.runes.haste;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.champions.weapons.impl.runes.SingleStatRune;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public abstract class HasteBase extends SingleStatRune {

    protected HasteBase(Champions plugin, String key) {
        super(plugin, key);
    }

    @Override
    public Component getRuneLoreDescription(ItemMeta itemMeta) {
        double roll = getRollFromMeta(itemMeta);
        return UtilMessage.deserialize("<gray>Increases attack speed by <green>%.1f%%",  roll);
    }

    @Override
    public Component getItemLoreDescription(PersistentDataContainer pdc) {
        int tier = pdc.getOrDefault(RuneNamespacedKeys.TIER, PersistentDataType.INTEGER, 0);
        double roll = pdc.getOrDefault(getAppliedNamespacedKey(), PersistentDataType.DOUBLE, 0.0);
        return UtilMessage.deserialize("<white>Haste %s <gray>- Increases attack speed by <green>%.1f%%", UtilFormat.getRomanNumeral(tier),  roll);
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
