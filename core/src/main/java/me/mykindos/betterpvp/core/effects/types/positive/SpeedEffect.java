package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.potion.PotionEffectType;

public class SpeedEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Speed";
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

    public String getGenericDescription() {
        return  "<white>" + getName() + "</white> increases movement speed by <green>20</green>% per level";
    }

    @Override
    public Component getGenericDescriptionComponent() {
        return Translations.component("core.effect.speed.generic",
                Component.text(getName(), NamedTextColor.WHITE),
                Component.text("20", NamedTextColor.GREEN));
    }

}

