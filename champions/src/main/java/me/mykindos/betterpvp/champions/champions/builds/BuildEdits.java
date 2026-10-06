package me.mykindos.betterpvp.champions.champions.builds;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillUpdateEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * The changes a player can make to their builds, shared by every build menu. Each one fires the build events, so
 * listeners see the same thing whichever menu made the change. Saving the build is left to the caller.
 */
@UtilityClass
public class BuildEdits {

    public static final int MIN_NAME_LENGTH = 3;
    public static final int MAX_NAME_LENGTH = 15;

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9 ]+");

    /**
     * Makes a build the role's active one.
     *
     * @return false if the player has no such build
     */
    public static boolean equip(Player player, GamerBuilds builds, Role role, int id) {
        final RoleBuild selected = builds.getBuild(role, id).orElse(null);
        if (selected == null) {
            return false;
        }
        final RoleBuild active = builds.getActiveBuilds().get(role.getName());
        active.setActive(false);
        selected.setActive(true);
        builds.getActiveBuilds().put(role.getName(), selected);
        UtilServer.callEvent(new ApplyBuildEvent(player, builds, active, selected));
        return true;
    }

    /**
     * Puts a skill into its slot at level 1, or raises it a level, replacing whatever other skill held the slot.
     *
     * @return false if the skill is disabled, already at its max level, or the build has no points left
     */
    public static boolean raise(Player player, RoleBuild build, Skill skill) {
        if (!skill.isEnabled()) {
            return false;
        }
        final RoleBuild previous = build.copy();
        BuildSkill current = build.getBuildSkill(skill.getType());
        if (current != null && !current.getSkill().equals(skill)) {
            build.setPoints(build.getPoints() + current.getLevel());
            build.setSkill(current.getSkill().getType(), null);
            UtilServer.callEvent(new SkillDequipEvent(player, current, build, previous));
            current = null;
        }
        if (build.getPoints() <= 0 || current != null && current.getLevel() >= skill.getMaxLevel()) {
            return false;
        }
        build.takePoint();
        if (current == null) {
            final BuildSkill added = new BuildSkill(skill, 1);
            build.setSkill(skill.getType(), added);
            UtilServer.callEvent(new SkillEquipEvent(player, added, build, previous));
        } else {
            build.setSkill(skill.getType(), skill, current.getLevel() + 1);
            UtilServer.callEvent(new SkillUpdateEvent(player, current, build, previous));
        }
        return true;
    }

    /**
     * Equips a skill in its slot, replacing the slot's skill, or takes it out when it is already equipped.
     *
     * @return false if the skill could not be equipped
     */
    public static boolean choose(Player player, RoleBuild build, Skill skill) {
        final BuildSkill current = build.getBuildSkill(skill.getType());
        if (current == null || !current.getSkill().equals(skill)) {
            return raise(player, build, skill);
        }
        final RoleBuild previous = build.copy();
        build.setSkill(skill.getType(), null);
        build.setPoints(build.getPoints() + current.getLevel());
        UtilServer.callEvent(new SkillDequipEvent(player, current, build, previous));
        return true;
    }

    /** Takes every skill out of the build and refunds its points, dequipping each so its effects stop. */
    public static void reset(Player player, RoleBuild build) {
        for (BuildSkill slotted : List.copyOf(build.getActiveSkills())) {
            final RoleBuild previous = build.copy();
            build.setSkill(slotted.getSkill().getType(), null);
            build.setPoints(build.getPoints() + slotted.getLevel());
            UtilServer.callEvent(new SkillDequipEvent(player, slotted, build, previous));
        }
    }

    /**
     * Fills the build at random: one random skill in each slot that has any.
     *
     * @param skills the skills the build's role can slot, by slot
     */
    public static void randomize(Player player, RoleBuild build, Map<SkillType, List<Skill>> skills) {
        reset(player, build);
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        skills.values().forEach(options -> {
            final List<Skill> enabled = options.stream().filter(Skill::isEnabled).toList();
            if (!enabled.isEmpty()) {
                raise(player, build, enabled.get(random.nextInt(enabled.size())));
            }
        });
    }

    public static boolean isValidName(String name) {
        return name.length() >= MIN_NAME_LENGTH && name.length() <= MAX_NAME_LENGTH && VALID_NAME.matcher(name).matches();
    }
}
