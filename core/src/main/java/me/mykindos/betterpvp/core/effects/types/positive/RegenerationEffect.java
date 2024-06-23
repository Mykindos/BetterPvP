package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.potion.PotionEffectType;

public class RegenerationEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Regeneration";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.REGENERATION;
    }

    @Override
    public String getDescription(int level) {
        //https://minecraft.wiki/w/Regeneration
        int ticks = (int) Math.max(Math.floor((50d/((level - 1)^2))), 1);
        double seconds = ticks / 20d;
        return "<white>" + getName() + " " + UtilFormat.getRomanNumeral(level) + "</white> increases health by an additional <stat>5</stat>% every <val>" + UtilFormat.formatNumber(seconds, 3) + "</val> seconds";
    }
}

