package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.potion.PotionEffectType;

public class StrengthEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Strength";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.STRENGTH;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Strength " + UtilFormat.getRomanNumeral(level) + " <reset>increases melee damage dealt by <stat>" + (level * 1.5) + "</stat>";
    }

    @Override
    public String getGenericDescription() {
        return  "<white>" + getName() + "</white> increases melee damage by <green>1.5</green> per level";
    }

    @Override
    public Component getGenericDescriptionComponent() {
        return Translations.component("core.effect.strength.generic",
                Component.text(getName(), NamedTextColor.WHITE),
                Component.text("1.5", NamedTextColor.GREEN));
    }

}

