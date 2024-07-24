package me.mykindos.betterpvp.champions.weapons.impl.runes.fortune;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.champions.weapons.impl.runes.SingleStatRune;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public abstract class FortuneBase extends SingleStatRune {

    protected FortuneBase(Champions plugin, String key) {
        super(plugin, key);
    }

    @Override
    public List<Component> getRuneLoreDescription(ItemMeta itemMeta) {
        double roll = getRollFromMeta(itemMeta);
        return new ArrayList<>(Arrays.asList(
                "<gray>Fish you catch are <green>%.1f%%<gray> heavier",
                "<gray>Logs you chop have <green>%.1f%%<gray> chance of doubling their drops"
        )).stream().map(string -> UtilMessage.deserialize(string, roll)).toList();
    }

    @Override
    public List<Component> getItemLoreDescription(PersistentDataContainer pdc, ItemStack itemStack) {
        int tier = pdc.getOrDefault(RuneNamespacedKeys.TIER, PersistentDataType.INTEGER, 0);
        double roll = pdc.getOrDefault(getAppliedNamespacedKey(), PersistentDataType.DOUBLE, 0.0);

        if (UtilItem.isAxe(itemStack)) {
            return List.of(UtilMessage.deserialize("%s <gray>- Chopped logs have a <green>%.1f%%<gray> chance of doubling their drops", getStarPrefix(tier), roll));
        }

        return List.of(UtilMessage.deserialize("%s <gray>- Caught fish are <green>%.1f%%<gray> heavier", getStarPrefix(tier), roll));
    }

    @Override
    public String[] getItemFilter() {
        return Rune.PROFESSION_TOOL_FILTER;
    }

    @Override
    public String getCategory() {
        return "fishing rods and axes";
    }

    @Override
    public NamespacedKey getAppliedNamespacedKey() {
        return RuneNamespacedKeys.FORTUNE;
    }

}
