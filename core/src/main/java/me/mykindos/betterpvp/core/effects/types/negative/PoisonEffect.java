package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.framework.statusbar.HealthBarPalette;
import me.mykindos.betterpvp.core.framework.statusbar.HealthBarTint;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.potion.PotionEffectType;

public class PoisonEffect extends VanillaEffectType implements HealthBarTint {

    private static final HealthBarPalette HEALTH_PALETTE = new HealthBarPalette(
            TextColor.color(96, 200, 70),   // green base
            TextColor.color(170, 255, 90),  // lime overflow
            TextColor.color(26, 60, 26));   // dark green empty

    @Override
    public String getName() {
        return "Poison";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.POISON;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Poison " + UtilFormat.getRomanNumeral(level) + "</white> deals <val>" + (level * 2) + "</val> damage every <stat>" + (25d/20d) + "</stat> seconds";
    }

    @Override
    public String getGenericDescription() {
        return "<white>" + getName() + "</white>" + " deals <green>2</green> damage per level every <yellow>" + (25d/20d) + "</yellow> seconds";
    }

    @Override
    public Component getGenericDescriptionComponent() {
        return Translations.component("core.effect.poison.generic",
                Component.text(getName(), NamedTextColor.WHITE),
                Component.text("3", NamedTextColor.GREEN),
                Component.text(String.valueOf(25d / 20d), NamedTextColor.YELLOW));
    }

    // Tint the health bar poison-green while active.
    @Override
    public HealthBarPalette healthBarTint() {
        return HEALTH_PALETTE;
    }

    @Override
    public int tintPriority() {
        return 10;
    }
}

