package me.mykindos.betterpvp.champions.weapons.impl.runes.fortune;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.champions.weapons.impl.runes.SingleStatRune;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;


public abstract class FortuneBase extends SingleStatRune {

    protected FortuneBase(Champions plugin, String key) {
        super(plugin, key);
    }

    @Override
    public List<Component> getRuneLoreDescription(ItemMeta itemMeta) {
        double roll = getRollFromMeta(itemMeta);
        return List.of(UtilMessage.deserialize("<gray>Fish you catch are <green>%.1f%%<gray> heavier", roll));
    }

    @Override
    public List<Component> getItemLoreDescription(PersistentDataContainer pdc) {
        int tier = pdc.getOrDefault(RuneNamespacedKeys.TIER, PersistentDataType.INTEGER, 0);
        double roll = pdc.getOrDefault(getAppliedNamespacedKey(), PersistentDataType.DOUBLE, 0.0);
        return List.of(UtilMessage.deserialize("%s <gray>- Caught fish are <green>%.1f%%<gray> heavier", getStarPrefix(tier), roll));
    }

    @Override
    public String[] getItemFilter() {
        return Rune.ROD_FILTER;
    }

    @Override
    public String getCategory() {
        return "fishing rods";
    }

    @Override
    public NamespacedKey getAppliedNamespacedKey() {
        return RuneNamespacedKeys.FORTUNE;
    }

}
