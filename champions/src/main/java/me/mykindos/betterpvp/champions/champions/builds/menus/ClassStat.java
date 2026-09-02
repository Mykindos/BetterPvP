package me.mykindos.betterpvp.champions.champions.builds.menus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;

import java.util.function.ToIntFunction;

/**
 * One of the three headline ratings a class is compared on in the class selector. Each is a hand-tuned 0-100
 * comparison between classes rather than a reading of any live attribute.
 */
@AllArgsConstructor
@Getter
public enum ClassStat {

    DAMAGE("damage", NamedTextColor.RED, Role::getDamageRating),
    SUPPORT("support", TextColor.color(85, 170, 255), Role::getSupportRating),
    MOBILITY("mobility", NamedTextColor.GREEN, Role::getMobilityRating);

    /**
     * The rating is drawn as five stars, so one star is one 20% step and the lore stars and the modelled
     * bar always agree.
     */
    public static final int STEPS = 5;

    private final String key;
    private final TextColor color;
    private final ToIntFunction<Role> rating;

    public Component getDisplayName() {
        return Translations.component("champions.menu.stat." + key + ".name");
    }

    public Component getDescription() {
        return Translations.component("champions.menu.stat." + key + ".description");
    }

    /**
     * This stat's rating for a role, rounded to the nearest 20% step.
     *
     * @param role the role to rate
     * @return the number of filled stars, 0 to {@link #STEPS}
     */
    public int getSteps(Role role) {
        return (int) ((rating.applyAsInt(role) / 100f) * STEPS);
    }

    public Color getDyeColor() {
        return Color.fromRGB(color.value());
    }
}
