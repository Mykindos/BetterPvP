package me.mykindos.betterpvp.champions.champions.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillUpdateEvent;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ChargeSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.FireSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.UtilitySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.WorldSkill;
import me.mykindos.betterpvp.champions.effects.ChampionsEffectTypes;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.function.IntToDoubleFunction;


@Singleton
@CustomLog
public abstract class Skill implements IChampionsSkill {

    protected final Champions champions;
    protected final ChampionsManager championsManager;

    private boolean enabled;
    private int maxLevel;
    protected double cooldown;
    protected double cooldownDecreasePerLevel;
    protected int energy;
    protected double energyDecreasePerLevel;
    protected double energyStartCost;
    protected double energyStartCostDecreasePerLevel;
    protected double baseCharge;
    protected double chargeIncreasePerLevel;

    private boolean canUseWhileSlowed;

    private boolean canUseWhileStunned;

    private boolean canUseWhileSilenced;

    private boolean canUseWhileLevitating;

    private boolean canUseInLiquid;

    @Inject
    public Skill(Champions champions, ChampionsManager championsManager) {
        this.champions = champions;
        this.championsManager = championsManager;
        loadConfig();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean canUseWhileSlowed() {
        return canUseWhileSlowed;
    }

    @Override
    public boolean canUseWhileStunned() {
        return canUseWhileStunned;
    }

    @Override
    public boolean canUseWhileSilenced() {
        return canUseWhileSilenced;
    }

    @Override
    public boolean canUseWhileLevitating() {
        return canUseWhileLevitating;
    }

    @Override
    public boolean canUseInLiquid() {
        return canUseInLiquid;
    }

    // Description rendering now provided directly by each skill via Component[] getDescription(int level)

    @Override
    public Component getTags() {
        Component component = Component.empty();
        if (this instanceof ChargeSkill) {
            component = component.append(tagComponent("charge", NamedTextColor.AQUA));
        }
        if (this instanceof PrepareArrowSkill) {
            component = component.append(tagComponent("arrow", NamedTextColor.DARK_BLUE));
        }

        if (this instanceof EnergyChannelSkill || this instanceof EnergySkill) {
            component = component.append(tagComponent("energy", NamedTextColor.YELLOW));
        }

        if (this instanceof ToggleSkill) {
            component = component.append(tagComponent("toggle", NamedTextColor.GRAY));
        }

        if (this instanceof CrowdControlSkill) {
            component = component.append(tagComponent("crowd-control", NamedTextColor.GOLD));
        }

        if (this instanceof DamageSkill) {
            component = component.append(tagComponent("damage", NamedTextColor.DARK_RED));
        }

        if (this instanceof MovementSkill) {
            component = component.append(tagComponent("movement", NamedTextColor.WHITE));
        }

        if (this instanceof AreaOfEffectSkill) {
            component = component.append(tagComponent("aoe", NamedTextColor.GOLD));
        }

        if (this instanceof BuffSkill) {
            component = component.append(tagComponent("buff", NamedTextColor.GREEN));
        }

        if (this instanceof DebuffSkill) {
            component = component.append(tagComponent("debuff", NamedTextColor.RED));
        }

        if (this instanceof OffensiveSkill) {
            component = component.append(tagComponent("offensive", NamedTextColor.LIGHT_PURPLE));
        }

        if (this instanceof DefensiveSkill) {
            component = component.append(tagComponent("defensive", NamedTextColor.GRAY));
        }

        if (this instanceof HealthSkill) {
            component = component.append(tagComponent("health", NamedTextColor.RED));
        }

        if (this instanceof FireSkill) {
            component = component.append(tagComponent("fire", NamedTextColor.YELLOW));
        }

        if (this instanceof TeamSkill) {
            component = component.append(tagComponent("team", NamedTextColor.AQUA));
        }

        if (this instanceof WorldSkill) {
            component = component.append(tagComponent("world", NamedTextColor.DARK_PURPLE));
        }

        if (this instanceof UtilitySkill) {
            component = component.append(tagComponent("utility", NamedTextColor.LIGHT_PURPLE));
        }

        if (component.equals(Component.empty())) {
            return null;
        }

        return component;
    }

    private Component tagComponent(String tag, NamedTextColor color) {
        return Translations.component("champions.skill.tag." + tag)
                .color(color)
                .appendSpace();
    }

    /**
     * The player-facing, translatable display name of this skill. This is resolved per-viewer at render
     * time from {@code champions.skill.<role>.<skill>.name} (mirroring the description key scheme), where
     * {@code <role>} is the class type (or {@code global} for class-less skills) and {@code <skill>} is the
     * kebab-cased {@link #getName()}. {@link #getName()} itself remains the stable internal identifier and
     * must not be used for display.
     *
     * @return the translatable display name component
     */
    public Component getDisplayName() {
        final String classPart = getClassType() != null ? getClassType().name().toLowerCase() : "global";
        final String skillPart = getName().toLowerCase().replace(" ", "-");
        return Translations.component("champions.skill." + classPart + "." + skillPart + ".name");
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    public void reload() {
        try {
            loadConfig();
        } catch (Exception ex) {
            log.error("Something went wrong loading the skill configuration for {}", getName(), ex).submit();
        }
    }

    private String getPath(String name) {
        String path;
        if (getClassType() != null) {
            path = "skills." + getClassType().name().toLowerCase() + "." + getName().toLowerCase().replace(" ", "") + "." + name;
        } else {
            path = "skills.global." + getName().toLowerCase().replace(" ", "") + "." + name;
        }
        return path;
    }

    protected <T> T getConfig(String name, T defaultValue, Class<T> type) {
        return champions.getConfig("skills/skills").getOrSaveObject(getPath(name), defaultValue, type);
    }

    /**
     * @param name         name of the value
     * @param defaultValue default value
     * @param type         The type of default value
     * @param <T>          The type of default value
     * @return returns the config value if exists, or the default value if it does not. Does not save value in the config
     */
    protected <T> T getConfigObject(String name, T defaultValue, Class<T> type) {
        return champions.getConfig("skills/skills").getObject(getPath(name), type, defaultValue);
    }


    @Override
    public final void loadConfig() {
        enabled = getConfig("enabled", true, Boolean.class);
        maxLevel = getConfig("maxlevel", 5, Number.class).intValue();

        if (this instanceof CooldownSkill) {
            cooldown = getConfig("cooldown", 1.0, Number.class).doubleValue();
            cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Number.class).doubleValue();
        }

        if (this instanceof EnergySkill || this instanceof EnergyChannelSkill) {
            energy = getConfig("energy", 0, Number.class).intValue();
            energyDecreasePerLevel = getConfig("energyDecreasePerLevel", 1.0, Number.class).doubleValue();
        }

        if (this instanceof ActiveToggleSkill) {
            energyStartCost = getConfig("energyStartCost", 10.0, Number.class).doubleValue();
            energyStartCostDecreasePerLevel = getConfig("energyStartCostDecreasePerLevel", 0.0, Number.class).doubleValue();
        }

        if (this instanceof ChargeSkill) {
            baseCharge = getConfig("baseCharge", 0.40, Number.class).doubleValue();
            chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 0.10, Number.class).doubleValue();
        }

        canUseWhileSlowed = getConfigObject("canUseWhileSlowed", true, Boolean.class);
        canUseWhileSilenced = getConfigObject("canUseWhileSilenced", false, Boolean.class);
        canUseWhileStunned = getConfigObject("canUseWhileStunned", false, Boolean.class);
        canUseWhileLevitating = getConfigObject("canUseWhileLevitating", false, Boolean.class);
        canUseInLiquid = getConfigObject("canUseInLiquid", false, Boolean.class);

        loadSkillConfig();
    }

    /**
     * @param method a method that takes the level
     * @param level  the level of the skill
     * @return A mini-message formatted string with the value to 2 decimal places
     */
    public String getValueString(IntToDoubleFunction method, int level) {
        return getValueString(method, level, 1);
    }

    public String getValueString(IntToDoubleFunction method, int level, int decimalPlaces) {
        return getValueString(method, level, 1, "", decimalPlaces);
    }

    /**
     * @param method        a method that takes the level
     * @param level         the level of the skill
     * @param multiplier    the multiplier to multiply the value by
     * @param decimalPlaces number of decimal places to use
     * @return A mini-message formatted string with the value
     */
    public String getValueString(IntToDoubleFunction method, int level, double multiplier, String suffix, int decimalPlaces) {
        double currentValue = method.applyAsDouble(level) * multiplier;
        double nextValue = method.applyAsDouble(level + 1) * multiplier;
        //if level is the same, it's a static value
        if (currentValue == nextValue) {
            return "<yellow>" + UtilFormat.formatNumber(currentValue, decimalPlaces, true) + "</yellow>" + suffix;
        }

        //it is a varying value, needs to be green
        String valueString = "<green>" + UtilFormat.formatNumber(currentValue, decimalPlaces, true) + "</green>" + suffix;

        if (level < getMaxLevel()) {
            double difference = nextValue - currentValue;
            if (difference > 0) {
                return valueString + " (+<green>" + UtilFormat.formatNumber(difference, decimalPlaces, true) + "</green>)";
            } else {
                difference = Math.abs(difference);
                return valueString + " (-<green>" + UtilFormat.formatNumber(difference, decimalPlaces, true) + "</green>)";
            }
        }
        return valueString;
    }

    /**
     * Returns a styled value {@link Component} for a level-dependent skill value.
     * Mirrors {@link #getValueString} colouring: values that change between this
     * level and the next render green, static values render yellow.
     *
     * @param method a method that takes the level
     * @param level  the level of the skill
     * @return a coloured component holding the formatted value
     */
    public Component getValueComponent(IntToDoubleFunction method, int level) {
        return getValueComponent(method, level, 1, 1);
    }

    /**
     * @param method        a method that takes the level
     * @param level         the level of the skill
     * @param decimalPlaces number of decimal places to use
     * @return a coloured component holding the formatted value
     */
    public Component getValueComponent(IntToDoubleFunction method, int level, int decimalPlaces) {
        return getValueComponent(method, level, 1, decimalPlaces);
    }

    /**
     * @param method        a method that takes the level
     * @param level         the level of the skill
     * @param multiplier    the multiplier to multiply the value by
     * @param decimalPlaces number of decimal places to use
     * @return a coloured component holding the formatted value
     */
    public Component getValueComponent(IntToDoubleFunction method, int level, double multiplier, int decimalPlaces) {
        double currentValue = method.applyAsDouble(level) * multiplier;
        double nextValue = method.applyAsDouble(level + 1) * multiplier;
        // if the value is the same next level it is static (yellow), otherwise it varies (green)
        NamedTextColor color = currentValue == nextValue ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
        return Component.text(UtilFormat.formatNumber(currentValue, decimalPlaces, true), color);
    }

    /**
     * As {@link #getValueComponent(IntToDoubleFunction, int, double, int)} but appends a literal
     * suffix (e.g. {@code "%"}) to the formatted value, keeping it the same colour as the value.
     *
     * @param method        a method that takes the level
     * @param level         the level of the skill
     * @param multiplier    the multiplier to multiply the value by
     * @param decimalPlaces number of decimal places to use
     * @param suffix        literal text appended after the value
     * @return a coloured component holding the formatted value and suffix
     */
    public Component getValueComponent(IntToDoubleFunction method, int level, double multiplier, int decimalPlaces, String suffix) {
        double currentValue = method.applyAsDouble(level) * multiplier;
        double nextValue = method.applyAsDouble(level + 1) * multiplier;
        // if the value is the same next level it is static (yellow), otherwise it varies (green)
        NamedTextColor color = currentValue == nextValue ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
        return Component.text(UtilFormat.formatNumber(currentValue, decimalPlaces, true) + suffix, color);
    }

    /**
     * Called when a player, for any reason, equips this skill.
     * i.e: Logging in, changing classes, editing build.
     *
     * @param player The player that equipped the skill
     * @param gamer
     */
    public void trackPlayer(Player player, Gamer gamer) {
    }

    /**
     * Called when a player, for any reason, unequips this skill.
     * i.e: Logging out, changing classes, editing build.
     *
     * @param player The player that unequipped the skill
     * @param gamer
     */
    public void invalidatePlayer(Player player, Gamer gamer) {
    }

    /**
     * Called when a skill is updated via {@link SkillUpdateEvent event}
     * @param player
     * @param gamer
     */
    public void updatePlayer(Player player, Gamer gamer) {

    }

    public void loadSkillConfig() {
    }

    public boolean hasSkill(Player player) {
        Optional<GamerBuilds> gamerBuildsOptional = championsManager.getBuilds().getObject(player.getUniqueId().toString());
        return gamerBuildsOptional.filter(this::hasSkill).isPresent();
    }

    /**
     * Check if a gamer has the current skill equipped
     *
     * @param builds The gamers builds
     * @return True if their
     */
    protected boolean hasSkill(GamerBuilds builds) {
        return getSkill(builds).isPresent();
    }

    protected Optional<BuildSkill> getSkill(Player player) {
        Optional<GamerBuilds> gamerBuildOptional = championsManager.getBuilds().getObject(player.getUniqueId().toString());
        if (gamerBuildOptional.isPresent()) {
            return getSkill(gamerBuildOptional.get());
        }
        return Optional.empty();
    }

    protected Optional<BuildSkill> getSkill(GamerBuilds gamerBuilds) {
        final Player player = Objects.requireNonNull(gamerBuilds.getClient().getGamer().getPlayer());
        Role role = championsManager.getRoles().getRole(player);
        if (role == getClassType() || getClassType() == null) {
            RoleBuild roleBuild = gamerBuilds.getActiveBuilds().get(role.getName());
            BuildSkill buildSkill = roleBuild.getBuildSkill(getType());
            if (buildSkill != null && buildSkill.getSkill() != null) {
                if (buildSkill.getSkill().equals(this)) {
                    return Optional.of(buildSkill);
                }
            }
        }

        return Optional.empty();
    }

    protected int getLevel(Player player) {
        Optional<BuildSkill> skillOptional = getSkill(player);
        int level = skillOptional.map(BuildSkill::getLevel).orElse(0);

        if (level == 0) return 0;

        //prevent passive skills working in spectator
        if (this instanceof PassiveSkill passiveSkill &&
                player.getGameMode() == GameMode.SPECTATOR &&
                !passiveSkill.enabledInSpectator()) {
            return 0;
        }

        // If its a passive that has no action, return standard level
        // Passives such as intimidation and backstab do not gain additional levels from boosters
        if(this.getType().isPassive() && !(this instanceof ToggleSkill)) {
            return level;
        }

        if (SkillWeapons.isHolding(player, getType()) && SkillWeapons.hasBooster(player)) {
            level++;
        }

        EffectType effectType = ChampionsEffectTypes.getBoostEffectForSkill(getType());
        if (effectType != null) {
            Optional<Effect> effectOptional = championsManager.getEffects().getEffect(player, effectType);
            if (effectOptional.isPresent()) {
                level += effectOptional.get().getAmplifier();
            }
        }

        return level;
    }


    @Override
    public boolean isHolding(Player player) {
        return hasSkill(player) && SkillWeapons.isHolding(player, getType());
    }
}
