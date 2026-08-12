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

    public double getDamage(long level) {
        return level * 2;
    }

    public double getInterval(long level) {
        return Math.max(0.75, 1.25 - (level - 1) * 0.25);
    }

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
        return "<white>Poison " + UtilFormat.getRomanNumeral(level) + "</white> deals <val>" + getDamage(level) + "</val> damage every <stat>" + getInterval(level) + "</stat> seconds";
    }

    @Override
    public String getGenericDescription() {
        return "<white>" + getName() + "</white>" + " deals <green>" + getDamage(1) + "</green> damage per level every <yellow>" + getInterval(1) + "</yellow> seconds";
    }

    @Override
    public Component getGenericDescriptionComponent() {
        return Translations.component("core.effect.poison.generic",
                Component.text(getName(), NamedTextColor.WHITE),
                Component.text(getDamage(1), NamedTextColor.GREEN),
                Component.text(getInterval(1), NamedTextColor.YELLOW));
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

