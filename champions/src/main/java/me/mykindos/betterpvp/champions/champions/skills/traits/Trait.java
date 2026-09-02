package me.mykindos.betterpvp.champions.champions.skills.traits;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A trait is an ability every player of a role has, all the time. Unlike a skill it is never chosen: it
 * occupies no build slot, costs no skill points, and does not appear in the skill menu. Traits are shown as
 * icons beneath the skill points in the build editor, and summarised under the health in the role selector.
 * <p>
 * A trait is still a {@link Skill}, so it keeps skill config loading, description rendering and tags. It only
 * differs in where its level comes from: a fixed, configured level held for as long as the role is equipped,
 * rather than a level bought in a build.
 */
public abstract class Trait extends Skill {

    private int level;

    protected Trait(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public final SkillType getType() {
        return SkillType.TRAIT;
    }

    /**
     * A trait always belongs to exactly one role, since it is what that role innately is.
     *
     * @return the role this trait belongs to
     */
    @Override
    public abstract @NotNull Role getClassType();

    /**
     * A single short line describing this trait, shown under the role's health in the role selector. Keep it
     * to a handful of words; the full description belongs in {@link #getDescription(int)}.
     *
     * @return the translatable summary component
     */
    public Component getSummary() {
        return Translations.component(traitKey("summary"));
    }

    @Override
    public Component getDisplayName() {
        return Translations.component(traitKey("name"));
    }

    /**
     * The level this trait is always held at.
     *
     * @return the configured trait level
     */
    public int getTraitLevel() {
        return level;
    }

    /**
     * A trait is held for as long as its role is equipped, so its level does not come from a build and is not
     * raised by skill boosters.
     */
    @Override
    protected int getLevel(Player player) {
        if (!isEnabled()) {
            return 0;
        }

        if (!championsManager.getRoles().hasRole(player, getClassType())) {
            return 0;
        }

        if (this instanceof PassiveSkill passiveSkill
                && player.getGameMode() == GameMode.SPECTATOR
                && !passiveSkill.enabledInSpectator()) {
            return 0;
        }

        return level;
    }

    @Override
    public boolean hasSkill(Player player) {
        return getLevel(player) > 0;
    }

    /**
     * Traits never scale, so their description values are always static and rendered yellow, rather than the
     * green {@link Skill#getValueComponent} uses for values that grow with the next level.
     *
     * @param value         the value to render
     * @param decimalPlaces number of decimal places to use
     * @return a coloured component holding the formatted value
     */
    protected Component traitValue(double value, int decimalPlaces) {
        return traitValue(value, decimalPlaces, "");
    }

    /**
     * As {@link #traitValue(double, int)} but appends a literal suffix (e.g. {@code "%"}) in the same colour.
     *
     * @param value         the value to render
     * @param decimalPlaces number of decimal places to use
     * @param suffix        literal text appended after the value
     * @return a coloured component holding the formatted value and suffix
     */
    protected Component traitValue(double value, int decimalPlaces, String suffix) {
        return Component.text(UtilFormat.formatNumber(value, decimalPlaces, true) + suffix, NamedTextColor.YELLOW);
    }

    /**
     * Reads this trait's description lines from {@code champions.trait.<role>.<trait>.description}.
     *
     * @param args MessageFormat arguments applied to every line
     * @return the description line components
     */
    protected Component[] traitDescription(ComponentLike... args) {
        return Translations.componentLines(traitKey("description"), args);
    }

    @Override
    public final void loadSkillConfig() {
        level = getConfig("level", 1, Integer.class);
        loadTraitConfig();
    }

    /**
     * Load this trait's own config values, as {@link Skill#loadSkillConfig()} does for skills.
     */
    protected void loadTraitConfig() {
    }

    @Override
    protected String getPath(String name) {
        return "traits." + getClassType().name().toLowerCase() + "." + getName().toLowerCase().replace(" ", "") + "." + name;
    }

    private String traitKey(String suffix) {
        return "champions.trait." + getClassType().name().toLowerCase() + "."
                + getName().toLowerCase().replace(" ", "-") + "." + suffix;
    }
}
