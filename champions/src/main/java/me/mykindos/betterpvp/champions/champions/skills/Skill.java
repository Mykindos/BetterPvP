package me.mykindos.betterpvp.champions.champions.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
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
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.UtilitySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.WorldSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@CustomLog
public abstract class Skill implements IChampionsSkill {

    protected final Champions champions;
    protected final ChampionsManager championsManager;

    private boolean enabled;
    @Getter
    protected double cooldown;
    protected int energy;
    protected double energyStartCost;

    private boolean canUseWhileSlowed;
    private boolean canUseWhileStunned;
    private boolean canUseWhileSilenced;
    private boolean canUseWhileLevitating;
    private boolean canUseInLiquid;

    @Inject
    protected Skill(Champions champions, ChampionsManager championsManager) {
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

    public Component[] parseDescription() {
        final String[] description = getDescription();
        final Component[] components = new Component[description.length];
        for (int i = 0; i < description.length; i++) {
            components[i] = MiniMessage.miniMessage().deserialize("<gray>" + description[i], SkillMenu.TAG_RESOLVER)
                    .decoration(TextDecoration.ITALIC, false);
        }
        return components;
    }

    @Override
    public Component getTags() {
        Component component = Component.empty();
        if (this instanceof PrepareArrowSkill) {
            component = component.append(Component.text("Arrow", NamedTextColor.DARK_BLUE).appendSpace());
        }

        if (this instanceof EnergyChannelSkill || this instanceof EnergySkill) {
            component = component.append(Component.text("Energy", NamedTextColor.YELLOW).appendSpace());
        }

        if (this instanceof ToggleSkill) {
            component = component.append(Component.text("Toggle", NamedTextColor.GRAY).appendSpace());
        }

        if (this instanceof CrowdControlSkill) {
            component = component.append(Component.text("Crowd Control", NamedTextColor.GOLD).appendSpace());
        }

        if (this instanceof DamageSkill) {
            component = component.append(Component.text("Damage", NamedTextColor.DARK_RED).appendSpace());
        }

        if (this instanceof MovementSkill) {
            component = component.append(Component.text("Movement", NamedTextColor.WHITE).appendSpace());
        }

        if (this instanceof AreaOfEffectSkill) {
            component = component.append(Component.text("AoE", NamedTextColor.GOLD).appendSpace());
        }

        if (this instanceof BuffSkill) {
            component = component.append(Component.text("Buff", NamedTextColor.GREEN).appendSpace());
        }

        if (this instanceof DebuffSkill) {
            component = component.append(Component.text("Debuff", NamedTextColor.RED).appendSpace());
        }

        if (this instanceof OffensiveSkill) {
            component = component.append(Component.text("Offensive", NamedTextColor.LIGHT_PURPLE).appendSpace());
        }

        if (this instanceof DefensiveSkill) {
            component = component.append(Component.text("Defensive", NamedTextColor.GRAY).appendSpace());
        }

        if (this instanceof HealthSkill) {
            component = component.append(Component.text("Health", NamedTextColor.RED).appendSpace());
        }

        if (this instanceof FireSkill) {
            component = component.append(Component.text("Fire", NamedTextColor.YELLOW).appendSpace());
        }

        if (this instanceof TeamSkill) {
            component = component.append(Component.text("Team", NamedTextColor.AQUA).appendSpace());
        }

        if (this instanceof WorldSkill) {
            component = component.append(Component.text("World", NamedTextColor.DARK_PURPLE).appendSpace());
        }

        if (this instanceof UtilitySkill) {
            component = component.append(Component.text("Utility", NamedTextColor.LIGHT_PURPLE).appendSpace());
        }

        if (component.equals(Component.empty())) {
            return null;
        }

        return component;
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

    protected <T> T getConfig(String name, Object defaultValue, Class<T> type) {
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

        if (this instanceof CooldownSkill) {
            cooldown = getConfig("cooldown", 1.0, Double.class);
        }

        if (this instanceof EnergySkill || this instanceof EnergyChannelSkill) {
            energy = getConfig("energy", 0, Integer.class);
        }

        if (this instanceof ActiveToggleSkill) {
            energyStartCost = getConfig("energyStartCost", 10.0, Double.class);
        }

        canUseWhileSlowed = getConfigObject("canUseWhileSlowed", true, Boolean.class);
        canUseWhileSilenced = getConfigObject("canUseWhileSilenced", false, Boolean.class);
        canUseWhileStunned = getConfigObject("canUseWhileStunned", false, Boolean.class);
        canUseWhileLevitating = getConfigObject("canUseWhileLevitating", false, Boolean.class);
        canUseInLiquid = getConfigObject("canUseInLiquid", true, Boolean.class);

        loadSkillConfig();
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

    public void loadSkillConfig() {
    }

    public boolean hasSkill(Player player) {
        Optional<GamerBuilds> gamerBuildsOptional = championsManager.getBuilds().getObject(player.getUniqueId());
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

    protected Optional<Skill> getSkill(Player player) {
        Optional<GamerBuilds> gamerBuildOptional = championsManager.getBuilds().getObject(player.getUniqueId());
        if (gamerBuildOptional.isEmpty()) {
            return Optional.empty();
        }
        return getSkill(gamerBuildOptional.get());
    }

    protected Optional<Skill> getSkill(GamerBuilds gamerBuilds) {
        Optional<Role> roleOptional = championsManager.getRoles().getObject(gamerBuilds.getUuid());
        if (roleOptional.isEmpty()) {
            return Optional.empty(); // No role
        }

        Role role = roleOptional.get();
        if (role != getClassType() && getClassType() != null) {
            return Optional.empty(); // Not the correct role
        }

        RoleBuild roleBuild = gamerBuilds.getActiveBuilds().get(role.getName());
        Skill skill = roleBuild.getSkill(getType());
        if (skill == null || !skill.equals(this)) {
            return Optional.empty(); // Not this skill
        }

        return Optional.of(skill);
    }

    @Override
    public boolean isHolding(Player player) {
        return hasSkill(player) && SkillWeapons.isHolding(player, getType());
    }

    @Override
    public Component toComponent() {
        Component descriptionComponent = Component.text(this.getName(), NamedTextColor.YELLOW);
        for (Component component : this.parseDescription()) {
            descriptionComponent = descriptionComponent.appendNewline().append(component);
        }

        return Component.text(this.getName(), NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/skilldescription " + this.getName()))
                .hoverEvent(HoverEvent.showText(descriptionComponent));
    }
}
