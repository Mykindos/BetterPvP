package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.wiki.types.IStaticWikiable;
import me.mykindos.betterpvp.core.wiki.types.WikiCategory;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SpeedEffect extends VanillaEffectType implements IStaticWikiable {

    @Override
    public String getName() {
        return "Speed";
    }

    @Override
    public WikiCategory getCategory() {
        return WikiCategory.EFFECTS;
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.SPEED;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Speed " + UtilFormat.getRomanNumeral(level) + " <reset>increases movement speed by <val>" + (level * 20) + "</val>%";
    }

    @Override
    public String getGenericDescription() {
        return  "<white>" + getName() + "</white> increases movement speed by <val>20</val>% per level";
    }

    /**
     * a list of mini-message formatted strings, to display in as the wiki item
     *
     * @return a list of strings, each string in the list is a line
     */
    @Override
    public List<String> getWikiDescription() {
        return List.of(getGenericDescription());
    }

    /**
     * The mini-message formatted title
     *
     * @return
     */
    @Override
    public String getTitle() {
        return "<green>Speed</green> (static test)";
    }

    /**
     * The base material to use as the item in the wiki
     *
     * @return
     */
    @Override
    public Material getDisplayMaterial() {
        return Material.SUGAR;
    }
}

